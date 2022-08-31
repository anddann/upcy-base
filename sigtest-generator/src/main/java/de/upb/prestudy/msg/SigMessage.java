package de.upb.prestudy.msg;

import de.upb.upcy.base.commons.ArtifactInfo;

public class SigMessage {
  public ArtifactInfo getArtifactInfo() {
    return artifactInfo;
  }

  public void setArtifactInfo(ArtifactInfo artifactInfo) {
    this.artifactInfo = artifactInfo;
  }

  private ArtifactInfo artifactInfo;
}
