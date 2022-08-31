package de.upb.upcy.base.updatesteps.graphanalyzer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class GraphResult {
  private String projectName;
  private String orgGav;

  private String duplicates;
  private int nrDuplicates = 0;

  private String conflicts;
  private int nrConflicts = 0;

  private String blossom;

  private int blossomSize = 0;
  private String srcDepending;
  private int nrSrcDepending = 0;
  private String abiDepending;
  private int nrAbiDepending = 0;

  public void setDuplicates(List<String> duplicates) {
    this.duplicates = String.join(";", duplicates);
    this.setNrDuplicates(duplicates.size());
  }

  public void setConflicts(List<String> conflicts) {
    this.conflicts = String.join(";", conflicts);
    this.setNrConflicts(conflicts.size());
  }

  public void setBlossom(List<String> blossom) {
    this.blossom = String.join(";", blossom);
    this.blossomSize = blossom.size();
  }

  public void setSrcDepending(List<String> srcDepending) {
    this.srcDepending = String.join(";", srcDepending);
    this.nrSrcDepending = srcDepending.size();
  }

  public void setAbiDepending(List<String> abiDepending) {
    this.abiDepending = String.join(";", abiDepending);
    this.nrAbiDepending = abiDepending.size();
  }
}
