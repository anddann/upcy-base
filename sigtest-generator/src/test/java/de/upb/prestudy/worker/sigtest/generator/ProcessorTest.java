package de.upb.prestudy.worker.sigtest.generator;

import static org.junit.Assert.assertNotNull;

import de.upb.upcy.base.commons.ArtifactInfo;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Ignore;
import org.junit.Test;

public class ProcessorTest {

  /** Check generation of sigfile for com.google.guava_guava_23.6-jre */
  @Test
  @Ignore
  public void testPerformAnalysis() throws IOException {
    final Path wadad = Files.createTempDirectory("wadad");
    ArtifactInfo artifactInfo = new ArtifactInfo();
    artifactInfo.setArtifactId("guava");
    artifactInfo.setP("jar");
    artifactInfo.setGroupId("com.google.guava");
    artifactInfo.setVersion("23.6-jre");
    artifactInfo.setRepoURL("https://repo1.maven.org/maven2/");

    Processor processor = new Processor(wadad);
    final Path path = processor.performAnalysis(artifactInfo);
    assertNotNull(path);
  }
}
