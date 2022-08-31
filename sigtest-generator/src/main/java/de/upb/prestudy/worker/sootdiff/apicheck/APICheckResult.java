package de.upb.prestudy.worker.sootdiff.apicheck;

public class APICheckResult {

  public enum APICheckType {
    CLASS,
    FIELD,
    METHOD,
    CONSTRUCTOR
  }

  private String prevElement;
  private String nextElement;
  private APICompatibility apiCompatibility;
  private String msg;

  public APICheckType getCheckType() {
    return checkType;
  }

  public void setCheckType(APICheckType checkType) {
    this.checkType = checkType;
  }

  private APICheckType checkType;

  public APICheckResult() {}

  public APICheckResult(
      String prev, String next, APICompatibility apiCompatibility, APICheckType checkedElement) {
    this.prevElement = prev;
    this.nextElement = next;
    this.apiCompatibility = apiCompatibility;
    this.checkType = checkedElement;
  }

  public String getPrevElement() {
    return prevElement;
  }

  public void setPrevElement(String prevElement) {
    this.prevElement = prevElement;
  }

  public String getNextElement() {
    return nextElement;
  }

  public void setNextElement(String nextElement) {
    this.nextElement = nextElement;
  }

  public APICompatibility getApiCompatibility() {
    return apiCompatibility;
  }

  public void setApiCompatibility(APICompatibility apiCompatibility) {
    this.apiCompatibility = apiCompatibility;
  }

  public void setDetailMessage(String msg) {
    this.msg = msg;
  }

  public String getDetailMessage() {
    return this.msg;
  }
}
