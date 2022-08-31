package de.upb.prestudy.db.model.sootdiff;

import de.upb.upcy.base.commons.ArtifactInfo;
import java.util.List;
import org.bson.types.ObjectId;

public class CallGraphCheckDoc {

  private ObjectId id;

  private ArtifactInfo baseArtifact;

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

  public int getNumberOfIncompatibleMethods() {
    return numberOfIncompatibleMethods;
  }

  public void setNumberOfIncompatibleMethods(int numberOfIncompatibleMethods) {
    this.numberOfIncompatibleMethods = numberOfIncompatibleMethods;
  }

  private ArtifactInfo nextArtifact;

  private int numberOfIncompatibleMethods;

  public List<MethodCGAPI> getBrokenMethodsSignature() {
    return brokenMethodsSignature;
  }

  public void setBrokenMethodsSignature(List<MethodCGAPI> brokenMethodsSignature) {
    this.brokenMethodsSignature = brokenMethodsSignature;
  }

  private List<MethodCGAPI> brokenMethodsSignature;

  public int getNumberOfEntryPointMethods() {
    return numberOfEntryPointMethods;
  }

  public void setNumberOfEntryPointMethods(int numberOfEntryPointMethods) {
    this.numberOfEntryPointMethods = numberOfEntryPointMethods;
  }

  private int numberOfEntryPointMethods;

  public static class MethodCGAPI {
    /**
     * The broken API Method (public, protected), and the first method in the CG with changed
     * SootDiffBody (or removed)
     */
    private String startMethod;

    private String changedBodyMethod;

    public String getStartMethod() {
      return startMethod;
    }

    public void setStartMethod(String startMethod) {
      this.startMethod = startMethod;
    }

    public String getChangedBodyMethod() {
      return changedBodyMethod;
    }

    public void setChangedBodyMethod(String changedBodyMethod) {
      this.changedBodyMethod = changedBodyMethod;
    }
  }
}
