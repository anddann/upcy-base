package de.upb.prestudy;

import de.upb.prestudy.db.MongoDBHandler;
import de.upb.prestudy.producer.generator.Producer;
import de.upb.prestudy.worker.sigtest.generator.Worker;
import de.upb.upcy.base.commons.ArtifactInfo;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class MainGenerateSigTest {
  private final MongoDBHandler mongoDBHandler = MongoDBHandler.getInstance();

  // done commons-io commons-io

  public static void main(String[] args) throws IOException {
    if (args.length == 1) {
      // check if its a file
      final Path path = Paths.get(args[0]);
      if (!Files.exists(path)) {
        System.exit(-1);
      } else {
        {
          MainGenerateSigTest mainGenerateSigTest = new MainGenerateSigTest();
          final List<String> lines = Files.readAllLines(path);
          for (String line : lines) {
            String[] split = line.split(",");
            if (split.length != 2) {
              continue;
            }
            mainGenerateSigTest.handle(split[0].trim(), split[1].trim());
          }
        }
      }
    } else if (args.length == 2) {
      String groupId = args[0];
      String artifactId = args[1];
      MainGenerateSigTest mainGenerateSigTest = new MainGenerateSigTest();
      mainGenerateSigTest.handle(groupId, artifactId);

    } else {
      System.exit(-1);
    }
  }

  public void handle(String groupId, String artifactId) throws IOException {
    System.out.println("Analyzing " + groupId + ":" + artifactId);
    Producer producer = new Producer();
    final List<ArtifactInfo> artifactInfos = producer.listOfGAVs(groupId, artifactId);
    this.handle(artifactInfos);
  }

  public void handle(List<ArtifactInfo> artifactInfos) throws IOException {
    Worker worker = new Worker(mongoDBHandler);
    worker.execute(artifactInfos);
  }

  public void handle(List<ArtifactInfo> artifactInfos, long timeout) throws IOException {
    Worker worker = new Worker(mongoDBHandler);
    worker.execute(artifactInfos);
  }
}
