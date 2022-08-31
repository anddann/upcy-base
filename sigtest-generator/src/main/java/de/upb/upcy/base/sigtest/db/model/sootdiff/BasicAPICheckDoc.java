package de.upb.upcy.base.sigtest.db.model.sootdiff;

import de.upb.upcy.base.commons.ArtifactInfo;
import de.upb.upcy.base.sigtest.worker.sootdiff.apicheck.APICheckResult;
import java.util.List;
import org.bson.types.ObjectId;

public class BasicAPICheckDoc {

  private ObjectId id;

  private ArtifactInfo baseArtifact;

  private ArtifactInfo nextArtifact;

  private int numberOfIncompatibleClasses;
  private int numberOfIncompatibleMethods;
  private int getNumberOfIncompatibleFields;
  private List<APICheckResult> apiCheckResultList;

  public List<APICheckResult> getApiCheckResultList() {
    return apiCheckResultList;
  }

  public void setApiCheckResultList(List<APICheckResult> apiCheckResultList) {
    this.apiCheckResultList = apiCheckResultList;
  }

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

  public int getNumberOfIncompatibleClasses() {
    return numberOfIncompatibleClasses;
  }

  public void setNumberOfIncompatibleClasses(int numberOfIncompatibleClasses) {
    this.numberOfIncompatibleClasses = numberOfIncompatibleClasses;
  }

  public int getNumberOfIncompatibleMethods() {
    return numberOfIncompatibleMethods;
  }

  public void setNumberOfIncompatibleMethods(int numberOfIncompatibleMethods) {
    this.numberOfIncompatibleMethods = numberOfIncompatibleMethods;
  }

  public int getGetNumberOfIncompatibleFields() {
    return getNumberOfIncompatibleFields;
  }

  public void setGetNumberOfIncompatibleFields(int getNumberOfIncompatibleFields) {
    this.getNumberOfIncompatibleFields = getNumberOfIncompatibleFields;
  }
}
