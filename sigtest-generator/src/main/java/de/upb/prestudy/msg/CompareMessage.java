package de.upb.prestudy.msg;

import de.upb.upcy.base.commons.ArtifactInfo;

public class CompareMessage {
  public ArtifactInfo getPrevArtifact() {
    return prevArtifact;
  }

  public void setPrevArtifact(ArtifactInfo prevArtifact) {
    this.prevArtifact = prevArtifact;
  }

  public ArtifactInfo getNextArtifact() {
    return nextArtifact;
  }

  public void setNextArtifact(ArtifactInfo nextArtifact) {
    this.nextArtifact = nextArtifact;
  }

  private ArtifactInfo prevArtifact;

  private ArtifactInfo nextArtifact;
}
