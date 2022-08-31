package de.upb.prestudy.db.model.check;

import java.util.ArrayList;
import java.util.List;

public class SigTestWarning {

  private String fqClassName;

  public String getFqClassName() {
    return fqClassName;
  }

  public void setFqClassName(String fqClassName) {
    this.fqClassName = fqClassName;
  }

  public List<String> getWarnings() {
    return warnings;
  }

  public void setWarnings(List<String> warnings) {
    this.warnings = warnings;
  }

  private List<String> warnings = new ArrayList<>();
}
