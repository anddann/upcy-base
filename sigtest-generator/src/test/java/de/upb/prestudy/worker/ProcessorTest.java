package de.upb.prestudy.worker;

import de.upb.upcy.base.commons.ArtifactInfo;
import de.upb.upcy.base.sigtest.worker.sigtest.generator.Processor;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

public class ProcessorTest {

  @Test
  public void jacksonTest() throws IOException {
    ArtifactInfo artifactInfo = new ArtifactInfo();
    artifactInfo.setArtifactId("junit");
    artifactInfo.setP("jar");
    artifactInfo.setGroupId("junit");
    artifactInfo.setVersion("4.13.2");
    artifactInfo.setRepoURL("https://repo1.maven.org/maven2/");

    final Path tempDirectory = Files.createTempDirectory(RandomStringUtils.randomAlphabetic(10));
    final Processor processor = new Processor(tempDirectory);
    processor.performAnalysis(artifactInfo);

    if (tempDirectory != null) {
      FileUtils.deleteDirectory(tempDirectory.toFile());
    }
  }
}
