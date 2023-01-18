package de.upb.prestudy.producer;

import com.fasterxml.jackson.databind.JsonNode;
import de.upb.upcy.base.mvn.MavenSearchAPIClient;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class MavenSearchAPIClientTest {

  @Test
  @Ignore // at 2023-01-13 Maven Central suffers from timeouts
  public void test() throws IOException {

    final JsonNode guice = MavenSearchAPIClient.getListOfArtifacts("com.google.inject", "guice");

    assertNotNull(guice);
    assertFalse(guice.isEmpty());
    System.out.println(guice);
  }
}
