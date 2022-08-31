package de.upb.prestudy.producer.generator;

import com.fasterxml.jackson.databind.JsonNode;
import de.upb.prestudy.mvn.MavenSearchAPIClient;
import de.upb.upcy.base.commons.ArtifactInfo;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.slf4j.LoggerFactory;

public class Producer {
  private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Producer.class);

  public List<ArtifactInfo> listOfGAVs(String groupId, String artifactId) throws IOException {
    final JsonNode response = MavenSearchAPIClient.getListOfArtifacts(groupId, artifactId);

    if (response == null) {
      return Collections.emptyList();
    }
    final JsonNode path = response.path("response").path("docs");
    if (path == null) {
      return Collections.emptyList();
    }
    final boolean array = path.isArray();
    if (array) {
      ArrayList<ArtifactInfo> artifactInfoArrayList = new ArrayList<>();
      for (Iterator<JsonNode> it = path.elements(); it.hasNext(); ) {
        JsonNode artifact = it.next();
        ArtifactInfo newArtifact = new ArtifactInfo();
        newArtifact.setRepoURL(MavenSearchAPIClient.MAVEN_CENTRAL);
        newArtifact.setGroupId(artifact.get("g").asText());
        newArtifact.setArtifactId(artifact.get("a").asText());
        newArtifact.setVersion(artifact.get("v").asText());
        newArtifact.setP(artifact.get("p").asText());
        artifactInfoArrayList.add(newArtifact);
      }
      LOGGER.info("Found {} artifacts", artifactInfoArrayList.size());
      return artifactInfoArrayList;
    }

    return Collections.emptyList();
  }
}
