package de.upb.upcy.base.sigtest.worker.sootdiff.apicheck;

public enum APICompatibility {
  V_DEC(false),
  ADD(true),
  DELETE(false),
  SUPER_TYPE_REMOVED(false),
  SIG_BREAK(false),
  BODY_BREAK(false),
  NON_API_CODE(true),
  NON_BREAKING(true);
  private boolean isCompatible;

  APICompatibility(boolean isAPI) {
    this.isCompatible = isAPI;
  }

  public boolean isCompatible() {
    return isCompatible;
  }
}
