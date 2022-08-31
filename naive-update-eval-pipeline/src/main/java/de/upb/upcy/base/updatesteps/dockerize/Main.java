package de.upb.upcy.base.updatesteps.dockerize;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Delivery;
import de.upb.upcy.base.build.Utils;
import de.upb.upcy.base.commons.RabbitMQCollective;
import de.upb.upcy.base.graphanalysis.MainGraphAnalysis;
import de.upb.upcy.base.updatesteps.buildnrunpipeline.Runner;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;
import org.apache.commons.lang3.tuple.Pair;
import org.codehaus.plexus.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main extends RabbitMQCollective {
  private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final Object READ_TIMEOUT = 1000000;
  private static final Object CONNECT_TIMEOUT = 10000000;
  private FileServerUpload httpClient;
  private ArrayList<String> doneProjectNames = new ArrayList<>();

  public Main() {
    super("MAVEN_PROJECT_PIPELINE");
  }

  public static void main(String[] args) throws Exception {
    Main main = new Main();
    main.run();
  }

  @Override
  protected void doWorkerJob(Delivery delivery) throws IOException {
    Msg msg = OBJECT_MAPPER.readValue(delivery.getBody(), Msg.class);
    try {
      LOGGER.info("[Worker] Received Request");
      if (doneProjectNames.contains(msg.getProjectName())) {
        LOGGER.info("Project {} is in done list.", msg.getProjectName());
      }

      LOGGER.info("[Worker] Running Build and Test Pipeline");

      final Path path = Utils.checkOutRepo(msg.getRepoUrl(), msg.getCommit());
      Path projectPom = path.resolve("pom.xml");
      Path tmpDir = Files.createTempDirectory(msg.getProjectName());
      Path csvOutPutDir = Files.createDirectory(tmpDir.resolve(msg.getProjectName()));

      Runner projectRun = new Runner(msg.getProjectName(), projectPom, csvOutPutDir);
      projectRun.run();

      LOGGER.info("[Worker] Done with build pipeline");

      try {
        LOGGER.info("[Worker] Running Graph Analysis");

        MainGraphAnalysis.main(new String[] {csvOutPutDir.toAbsolutePath().toString()});

        LOGGER.info("[Worker] Done Graph Analysis");
      } catch (IOException exception) {
        Files.write(csvOutPutDir.resolve("graph_error"), exception.getMessage().getBytes());
      }

      LOGGER.info("[Worker] Uploading results files");

      String timeStamp = new SimpleDateFormat("yyyyMMddHHmm").format(new Date());

      final String zipFileName = msg.getProjectName() + "_" + timeStamp + ".zip";
      final ZipFile zipFile = new ZipFile(zipFileName);
      zipFile.addFolder(csvOutPutDir.toFile());

      httpClient.uploadFileWebDav(zipFile.getFile());
      LOGGER.info("[Worker] Uploading files done");

    } catch (Exception e) {
      LOGGER.error("[Worker] Failed Crawling  with", e);
    }
  }

  @Override
  protected void doProducerJob(AMQP.BasicProperties props) throws Exception {
    final File destination = new File("projects.zip");
    final String projects_download_url = System.getenv("PROJECTS_DOWNLOAD_URL");
    LOGGER.info("Downloading... {}", projects_download_url);
    FileUtils.copyURLToFile(new URL(projects_download_url), destination);
    LOGGER.info("Done download: {}", projects_download_url);
    LOGGER.info("Unzip: {}", destination);

    final ZipFile zipFile = new ZipFile(destination);
    List<FileHeader> fileHeaders = zipFile.getFileHeaders();
    fileHeaders.stream()
        .filter(f -> f.getFileName().endsWith("COMMIT"))
        .forEach(
            x -> {
              try {
                zipFile.extractFile(x, "projectDir");
              } catch (ZipException e) {
                //
              }
            });

    final Path projectDir = Paths.get("projectDir", "projects");
    final Set<Path> commitFiles = Utils.findCommitFiles(projectDir.toAbsolutePath().toString());
    for (Path commitFile : commitFiles) {

      final String projectName =
          String.format(
              "%s_%s",
              commitFile.getParent().getParent().getFileName(),
              commitFile.getParent().getFileName());
      final Pair<String, String> repoAndCommit = Utils.getRepoAndCommit(commitFile);

      Msg msg = new Msg();
      msg.setProjectName(projectName);
      msg.setRepoUrl(repoAndCommit.getLeft());
      msg.setCommit(repoAndCommit.getRight());
      LOGGER.info("[Producer] Enqueue: {}", msg);
      String jsonString = OBJECT_MAPPER.writeValueAsString(msg);
      this.enqueue(props, jsonString.getBytes());
    }
  }

  @Override
  protected void preFlightCheck() {
    if (this.isWorkerNode()) {
      this.httpClient =
          new FileServerUpload(
              System.getenv("FILESERVER_HOST"),
              System.getenv("FILESERVER_USER"),
              System.getenv("FILESERVER_PASS"));

      // donwload projects file
      try {
        final Path target = Paths.get("done_projects.txt");
        httpClient.getFileWebDav("done_projects.txt", target);

        try (BufferedReader br = Files.newBufferedReader(target)) {
          String line;
          while ((line = br.readLine()) != null) {
            doneProjectNames.add(line.trim());
          }
        }
      } catch (IOException exception) {
        LOGGER.error("Failed to download project file", exception);
      }
    }
  }

  @Override
  protected void shutdown() {
    if (this.isWorkerNode() && this.httpClient != null) {

      this.httpClient.close();
    }
    // nothing

    LOGGER.info("Shutdown");
  }
}
