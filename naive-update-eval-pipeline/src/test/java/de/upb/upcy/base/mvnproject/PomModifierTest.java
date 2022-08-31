package de.upb.upcy.base.mvnproject;

import de.upb.upcy.base.updatesteps.PomModifier;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import org.dom4j.DocumentException;
import org.junit.Test;

public class PomModifierTest {

  @Test
  public void testPomParsing() throws IOException, DocumentException {
    final ClassLoader classLoader = this.getClass().getClassLoader();
    final String filename = "simple.xml";
    final URL resource = classLoader.getResource(filename);
    final Path tempDirectory = Files.createTempDirectory("dummy");
    Files.copy(
        Paths.get(resource.getFile()),
        tempDirectory.resolve(filename),
        StandardCopyOption.REPLACE_EXISTING);

    // work on the copy

    PomModifier pomModifier = new PomModifier(tempDirectory.resolve(filename));
    pomModifier.addOrUpdate("junit", "junit", "5.0");

    System.out.println(tempDirectory);
  }
}
