package de.upb.upcy.base.graph;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GraphModel {

  private String graphName;

  private List<Artifact> artifacts = new ArrayList<>();

  private List<Dependency> dependencies = new ArrayList<>();

  public enum ResolutionType {
    INCLUDED,
    OMITTED_FOR_DUPLICATE,
    OMITTED_FOR_CONFLICT,
    PARENT
  }

  @JsonInclude(JsonInclude.Include.ALWAYS)
  @JsonIgnoreProperties(ignoreUnknown = true)
  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  public static class Artifact {
    private String id;
    private int numericId;
    private String groupId;
    private String artifactId;
    private String version;
    private boolean optional;
    private List<String> scopes = new ArrayList<>();
    private List<String> types = new ArrayList<>();

    public String toGav() {
      return this.groupId + ":" + this.artifactId + ":" + this.version;
    }
  }

  @JsonInclude(JsonInclude.Include.ALWAYS)
  @JsonIgnoreProperties(ignoreUnknown = true)
  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  public static class Dependency {
    private String from;
    private String to;
    private int numericFrom;
    private int numericTo;

    private ResolutionType resolution;
  }
}
