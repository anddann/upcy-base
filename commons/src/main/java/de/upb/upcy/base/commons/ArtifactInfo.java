package de.upb.upcy.base.commons;

import java.util.Objects;

public class ArtifactInfo {
  private String p;
  private String repoURL;
  private String groupId;
  private String artifactId;
  private String version;
  private String classifier;

  public String getRepoURL() {
    return repoURL;
  }

  public void setRepoURL(String repoURL) {
    this.repoURL = repoURL;
  }

  public String getP() {
    return p;
  }

  public void setP(String p) {
    this.p = p;
  }

  public String getGroupId() {
    return groupId;
  }

  public void setGroupId(String groupId) {
    this.groupId = groupId;
  }

  public String getArtifactId() {
    return artifactId;
  }

  public void setArtifactId(String artifactId) {
    this.artifactId = artifactId;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getClassifier() {
    return classifier;
  }

  public void setClassifier(String classifier) {
    this.classifier = classifier;
  }

  public String toString() {
    return String.format("%s_%s_%s", groupId, artifactId, version);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ArtifactInfo that = (ArtifactInfo) o;
    return Objects.equals(p, that.p)
        && Objects.equals(repoURL, that.repoURL)
        && Objects.equals(groupId, that.groupId)
        && Objects.equals(artifactId, that.artifactId)
        && Objects.equals(version, that.version)
        && Objects.equals(classifier, that.classifier);
  }

  @Override
  public int hashCode() {
    return Objects.hash(p, repoURL, groupId, artifactId, version, classifier);
  }
}
