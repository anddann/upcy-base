package de.upb.upcy.base.sigtest.db.model.check;

import de.upb.upcy.base.commons.ArtifactInfo;
import org.bson.types.ObjectId;

public class SigTestCheckDBDoc {

  private ObjectId id;

  private ArtifactInfo baseArtifact;

  private ArtifactInfo nextArtifact;

  private String reportFileContent;

  private int numberOfIncompatibleClasses;

  public int getNumberOfIncompatibleClasses() {
    return numberOfIncompatibleClasses;
  }

  public void setNumberOfIncompatibleClasses(int numberOfIncompatibleClasses) {
    this.numberOfIncompatibleClasses = numberOfIncompatibleClasses;
  }

  public int getNumberOfIncompatibleConstructs() {
    return numberOfIncompatibleConstructs;
  }

  public void setNumberOfIncompatibleConstructs(int numberOfIncompatibleConstructs) {
    this.numberOfIncompatibleConstructs = numberOfIncompatibleConstructs;
  }

  private int numberOfIncompatibleConstructs;

  public ObjectId getId() {
    return id;
  }

  public void setId(ObjectId id) {
    this.id = id;
  }

  public ArtifactInfo getBaseArtifact() {
    return baseArtifact;
  }

  public void setBaseArtifact(ArtifactInfo baseArtifact) {
    this.baseArtifact = baseArtifact;
  }

  public ArtifactInfo getNextArtifact() {
    return nextArtifact;
  }

  public void setNextArtifact(ArtifactInfo nextArtifact) {
    this.nextArtifact = nextArtifact;
  }

  public String getReportFileContent() {
    return reportFileContent;
  }

  public void setReportFileContent(String reportFileContent) {
    this.reportFileContent = reportFileContent;
  }
}
