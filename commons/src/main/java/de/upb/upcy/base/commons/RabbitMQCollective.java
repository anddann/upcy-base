package de.upb.upcy.base.commons;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.AMQP.BasicProperties.Builder;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Delivery;
import com.rabbitmq.client.Envelope;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class RabbitMQCollective {
  private static final Logger logger = LoggerFactory.getLogger(RabbitMQCollective.class);
  public static final String DEFAULT_RABBITMQ_REPLY_TO = "amq.rabbitmq.reply-to";
  public static final int PREFETCH_COUNT = 1;
  private final String queueName;
  private final String replyQueue;
  private final int queue_length;
  private final boolean workerNode;
  private final String rabbitmqHost;
  private ArrayBlockingQueue<Object> actor_queue;
  private Channel activeChannel;

  public static String getRabbitMQHostFromEnvironment() {
    String res = System.getenv("RABBITMQ_HOST");
    if (res == null || res.isEmpty()) {
      res = "localhost";
    }
    return res;
  }

  public static boolean getWorkerNodeFromEnvironment() {
    String res = System.getenv("WORKER_NODE");
    boolean workerNode;
    if (res != null && !res.isEmpty()) {
      workerNode = Boolean.parseBoolean(res);
    } else {
      workerNode = true;
    }
    return workerNode;
  }

  public static int getActorLimit() {
    String res = System.getenv("ACTOR_LIMIT");
    int actorLimit;
    if (res != null && !res.isEmpty()) {
      actorLimit = Integer.parseInt(res);
    } else {
      actorLimit = 20;
    }
    return actorLimit;
  }

  public RabbitMQCollective(String queueName) {
    this(
        queueName,
        getRabbitMQHostFromEnvironment(),
        getWorkerNodeFromEnvironment(),
        "amq.rabbitmq.reply-to",
        getActorLimit());
  }

  public RabbitMQCollective(
      String queueName,
      String rabbitmqHost,
      boolean workerNode,
      String replyQueueName,
      int queue_length) {
    this.workerNode = workerNode;
    this.rabbitmqHost = rabbitmqHost;
    this.queueName = queueName;
    this.replyQueue = replyQueueName;
    this.queue_length = queue_length;
    logger.info("rabbitmqHost: {}", rabbitmqHost);
    logger.info("workerNode: {}", workerNode);
    logger.info("ACTOR_LIMIT: {}", queue_length);
  }

  protected void runWorker(Channel channel) throws IOException, TimeoutException {
    DeliverCallback deliverCallback =
        (consumerTag, delivery) -> {
          try {
            this.doWorkerJob(delivery);
          } catch (Exception var8) {
            logger.error("[Worker] job failed...", var8);
          } finally {
            channel.basicPublish(
                "",
                delivery.getProperties().getReplyTo(),
                (BasicProperties) null,
                "Polo".getBytes());
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            logger.info("[Worker] Send Ack");
          }
        };
    channel.basicConsume(this.getQueueName(), false, deliverCallback, (consumerTag) -> {});
  }

  protected abstract void doWorkerJob(Delivery var1) throws IOException;

  protected void runProducer(Channel channel) throws Exception {
    this.setupResponseListener(channel);
    BasicProperties props = (new Builder()).replyTo(this.getReplyQueue()).build();
    this.doProducerJob(props);
  }

  protected abstract void doProducerJob(BasicProperties var1) throws Exception;

  private void setupResponseListener(Channel channel) throws IOException {
    final BlockingQueue<String> response = new ArrayBlockingQueue(1);
    channel.basicConsume(
        this.getReplyQueue(),
        true,
        new DefaultConsumer(channel) {
          public void handleDelivery(
              String consumerTag, Envelope envelope, BasicProperties properties, byte[] body)
              throws UnsupportedEncodingException {
            RabbitMQCollective.logger.info("[Producer] Received Ack");
            response.offer(new String(body, StandardCharsets.UTF_8));
            RabbitMQCollective.this.actor_queue.poll();
            RabbitMQCollective.logger.info("[Producer] Removed Element from Queue");
          }
        });
  }

  protected void run() throws Exception {
    this.preFlightCheck();
    this.activeChannel = this.createChannel();
    if (!this.workerNode) {
      this.actor_queue = new ArrayBlockingQueue(this.queue_length);
      this.runProducer(this.activeChannel);
      logger.error("[Producer] Producer finished");
    } else {
      this.runWorker(this.activeChannel);
      logger.error("[Worker] Worker finished");
    }
  }

  protected abstract void preFlightCheck();

  private Channel createChannel() throws IOException, TimeoutException {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost(this.rabbitmqHost);
    Connection connection = factory.newConnection();
    Channel channel = connection.createChannel();
    channel.queueDeclare(this.queueName, false, false, false, (Map) null);
    channel.basicQos(1);
    return channel;
  }

  public String getReplyQueue() {
    return this.replyQueue;
  }

  public String getQueueName() {
    return this.queueName;
  }

  public void enqueue(BasicProperties props, byte[] body) throws IOException, InterruptedException {
    this.actor_queue.put(body);
    this.activeChannel.basicPublish("", this.getQueueName(), props, body);
  }

  public boolean isWorkerNode() {
    return this.workerNode;
  }
}
