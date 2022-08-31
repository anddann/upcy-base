package de.upb.upcy.base.sigtest.db.model.generate;

import de.upb.upcy.base.commons.ArtifactInfo;
import de.upb.upcy.base.sigtest.db.model.SimpleClassDescriptor;
import java.util.List;
import org.bson.types.ObjectId;

public class SigTestDBDoc {

  private ObjectId id;

  private ArtifactInfo artifactInfo;

  private String sigTestFileContent;

  public List<SimpleClassDescriptor> getClassDescriptions() {
    return classDescriptions;
  }

  public void setClassDescriptions(List<SimpleClassDescriptor> classDescriptions) {
    this.classDescriptions = classDescriptions;
  }

  // FIXME -- pojo fails, thus create a flatt class model... :(
  private List<SimpleClassDescriptor> classDescriptions;

  public ObjectId getId() {
    return id;
  }

  public void setId(ObjectId id) {
    this.id = id;
  }

  public ArtifactInfo getArtifactInfo() {
    return artifactInfo;
  }

  public void setArtifactInfo(ArtifactInfo artifactInfo) {
    this.artifactInfo = artifactInfo;
  }

  public String getSigTestFileContent() {
    return sigTestFileContent;
  }

  public void setSigTestFileContent(String sigTestFileContent) {
    this.sigTestFileContent = sigTestFileContent;
  }
}
