package de.upb.upcy.base.sigtest;

import de.upb.upcy.base.commons.ArtifactInfo;
import de.upb.upcy.base.commons.ArtifactUtils;
import de.upb.upcy.base.sigtest.producer.generator.Producer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class MainGoOffline {

  public static void main(String[] args) throws IOException {
    if (args.length == 1) {
      // check if its a file
      final Path path = Paths.get(args[0]);
      if (!Files.exists(path)) {
        System.exit(-1);
      } else {
        MainGoOffline mainGenerateSigTest = new MainGoOffline();
        final List<String> lines = Files.readAllLines(path);
        for (String line : lines) {
          String[] split = line.split(",");
          if (split.length != 2) {
            continue;
          }
          mainGenerateSigTest.handle(split[0].trim(), split[1].trim());
        }
      }
    } else if (args.length == 2) {
      String groupId = args[0];
      String artifactId = args[1];
      MainGoOffline mainGenerateSigTest = new MainGoOffline();
      mainGenerateSigTest.handle(groupId, artifactId);

    } else {
      System.exit(-1);
    }
  }

  private void handle(String groupId, String artifactId) throws IOException {
    System.out.println("Analyzing " + groupId + ":" + artifactId);
    Producer producer = new Producer();
    final List<ArtifactInfo> artifactInfos = producer.listOfGAVs(groupId, artifactId);

    final Path test = Files.createTempDirectory("test");

    for (ArtifactInfo artifactInfo : artifactInfos) {

      ArtifactUtils.downloadFilePlainURL(artifactInfo, test);
    }
  }
}
