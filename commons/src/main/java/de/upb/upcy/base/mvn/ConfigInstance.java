package de.upb.upcy.base.mvn;

public class ConfigInstance {
  private static ConfigInstance instance;

  public static ConfigInstance instance() {
    if (instance == null) {
      instance = new ConfigInstance();
    }
    return instance;
  }

  public String getCacheFolder() {
    return System.getenv("CACHE_DIR") == null ? "./cache" : System.getenv("CACHE_DIR");
  }

  /**
   * The number of libraries to update per project
   *
   * @return
   */
  public int getNumberOfLibsToUpdate() {
    return 6;
  }

  /**
   * The number of new versions to try out
   *
   * @return
   */
  public int getNumberOfUpdatesToConsider() {
    return 6;
  }

  public int getNumberOfThreads() {
    return System.getenv("NUM_THREADS") == null
        ? 1
        : Integer.parseInt(System.getenv("NUM_THREADS"));
  }

  public String getLocalM2Repository() {
    // -Dmaven.repo.local=$HOME/.my/other/repository
    return System.getenv("M2_DIR");
  }
}
