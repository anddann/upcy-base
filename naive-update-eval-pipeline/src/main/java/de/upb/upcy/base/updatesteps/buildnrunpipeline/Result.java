package de.upb.upcy.base.updatesteps.buildnrunpipeline;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.opencsv.bean.CsvBindByName;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Result {
  @CsvBindByName private String projectName;

  @CsvBindByName private int inDegree = 0;

  @CsvBindByName private String orgGav;

  @CsvBindByName private String newGav;

  @CsvBindByName private boolean transitive = false;

  @CsvBindByName private OUTCOME buildResult;

  @CsvBindByName private OUTCOME testResult;

  @CsvBindByName private String buildError;

  @CsvBindByName private String testErrors;

  @CsvBindByName private int nrtestErrors = 0;

  @CsvBindByName private String testFailures;

  @CsvBindByName private int nrtestFailures = 0;

  @CsvBindByName private int nrOfNewerVersions = 0;

  public void setTestErrors(List<String> testErrors) {
    this.testErrors = String.join(";", testErrors);
    this.setNrtestErrors(testErrors.size());
  }

  public void setTestFailures(List<String> testFailures) {
    this.testFailures = String.join(";", testFailures);

    this.setNrtestFailures(testFailures.size());
  }

  public enum OUTCOME {
    SKIP,
    NO_UPDATES,
    FAIL,
    SUCCESS
  }
}
