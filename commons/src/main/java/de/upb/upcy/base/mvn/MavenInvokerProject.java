package de.upb.upcy.base.mvn;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Joiner;
import com.google.common.base.Stopwatch;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class MavenInvokerProject {

  private static final Pattern CP_PATTERN =
      Pattern.compile("(classpath|testClasspath|classpathElements|compilePath) = \\[(.*)\\]");
  private static final Pattern OUTDIR_PATTERN = Pattern.compile("(outputDirectory) = (.*)");
  private static final Pattern SOURCE_PATTERN =
      Pattern.compile("(compileSourceRoots) = \\[(.*)\\]");
  private static final Pattern RUNTIME_PATTERN = Pattern.compile("(java.home)=(.*),");
  private static final Logger LOGGER = LoggerFactory.getLogger(MavenInvokerProject.class);
  protected static final int TIMEOUT_IN_SECONDS = -1;

  @JsonProperty protected Path buildFile;

  @JsonProperty
  private Collection<String> fullProjectClassPath,
      targetDirectories,
      sourceDirectories,
      runtimeDir = new ArrayList<>();

  @JsonProperty private boolean compiled = false;
  @JsonProperty private boolean initialized = false;

  @JsonProperty private int exitCode;
  @JsonProperty private String output;
  @JsonProperty private String error;

  /** Just here for jackson deserialization */
  @Deprecated
  public MavenInvokerProject() {}

  /** @param buildFile The pom file of the represented Maven project */
  public MavenInvokerProject(Path buildFile) {
    this.buildFile = buildFile;
    LOGGER.info("Maven project discovered: '{}'", buildFile);
  }

  public static boolean useInteractiveBash() {
    return System.getenv("USEBASH") == null ? true : Boolean.parseBoolean(System.getenv("USEBASH"));
  }

  public static Triple<Integer, String, String> runCommand(
      Path pomPath, int timeOutInSeconds, String... commands) throws BuildToolException {

    // TODO derive java version from pom file (running a java 8 project with jdk 11 does not
    // work)....

    try {
      Stopwatch watch = Stopwatch.createStarted();

      // warning! do not include -X here. callers of this method might expect certain output that
      // does not contain debug logs. append it in caller if needed!
      final String localM2Repository = ConfigInstance.instance().getLocalM2Repository();
      String[] goals;
      if (!StringUtils.isBlank(localM2Repository)) {
        goals =
            ArrayUtils.addAll(
                new String[] {"mvn", "-B", "-e", "-fae", "-Dmaven.repo.local=" + localM2Repository},
                commands);

      } else {
        goals = ArrayUtils.addAll(new String[] {"mvn", "-B", "-e", "-fae"}, commands);
      }

      // run the command via a bash as the current logged in user to make sure we can find 'pwd' and
      // other unix tools

      goals = new String[] {"bash", "-l", "-c", String.join(" ", goals)};

      ProcessBuilder processBuilder = new ProcessBuilder(goals);
      processBuilder.directory(pomPath.getParent().toFile());

      final Triple<Integer, String, String> processRetCodeOutErr =
          IOUtils.awaitTermination(processBuilder.start(), timeOutInSeconds);

      int exitCode = processRetCodeOutErr.getLeft();
      String output = processRetCodeOutErr.getMiddle();
      String error = processRetCodeOutErr.getRight();

      LOGGER.trace(
          "Executing maven command '{}' with '{}' took {}",
          Joiner.on(" ").join(goals),
          pomPath,
          watch);

      if (exitCode == IOUtils.TIMEOUT_EXITCODE) {
        throw new BuildToolException(
            String.format(
                "The Maven process timed out when executing '%s'.\nStdout: %s\nStderr: %s",
                Joiner.on(" ").join(goals), output, error));
      } else if (exitCode != 0) {
        throw new BuildToolException(
            String.format(
                "Maven returned a non-zero exit code %d when executing '%s'.\nStdout: %s\nStderr: %s",
                exitCode, Joiner.on(" ").join(goals), output, error));
      }

      return Triple.of(exitCode, output, error);
    } catch (ExecutionException | InterruptedException | IOException e) {
      throw new BuildToolException(
          String.format("Unable to invoke '%s'", Joiner.on(" ").join(commands)), e);
    }
  }

  public static int getTimeOutInSeconds() {
    return TIMEOUT_IN_SECONDS;
  }

  @JsonProperty("fullProjectClassPath")
  @JsonIgnore
  public Collection<String> getClassPath() {
    //  Preconditions.checkState(initialized, "Project not initialized...");
    return fullProjectClassPath;
  }

  public Collection<String> getTargetDirectories() {
    // Preconditions.checkState(initialized, "Project not initialized...");
    return targetDirectories;
  }

  public Collection<String> getSourceDirectories() {
    //  Preconditions.checkState(initialized, "Project not initialized...");
    return sourceDirectories;
  }

  public boolean isCompiled() {
    return compiled;
  }

  public boolean isInitialized() {
    return initialized;
  }

  /**
   * Compiles this project. Also initializes the project, if initialize was not called before...
   *
   * @throws BuildToolException
   */
  public Triple<Integer, String, String> compile() throws BuildToolException {

    initialize();

    if (isCompiled()) {
      return Triple.of(exitCode, output, error);
    }

    final Triple<Integer, String, String> compile =
        runCommand(buildFile, getTimeOutInSeconds(), "compile", "--fail-at-end");
    this.exitCode = compile.getLeft();
    this.output = compile.getMiddle();
    this.error = compile.getRight();
    compiled = true;

    return compile;
  }

  public Triple<Integer, String, String> runCmd(String... commands) throws BuildToolException {
    return MavenInvokerProject.runCommand(buildFile, getTimeOutInSeconds(), commands);
  }

  public void initialize() throws BuildToolException {
    if (initialized) {
      return;
    }

    // we will get everything from a single maven run, so let's configure it
    ArrayList<String> commandList = new ArrayList<>();
    commandList.add("clean");

    boolean isIncludeTests = false;
    String goal = isIncludeTests ? "test-compile" : "compile";
    commandList.add(goal);

    // give us all the information
    commandList.add("-X");
    commandList.add("--fail-at-end");

    String output =
        runCommand(buildFile, getTimeOutInSeconds(), commandList.toArray(new String[0]))
            .getMiddle();

    this.fullProjectClassPath = pathForPattern(output, CP_PATTERN);
    LOGGER.debug("Classpath computed for '{}': '{}'", this, fullProjectClassPath);

    this.targetDirectories = pathForPattern(output, OUTDIR_PATTERN);
    LOGGER.debug("Target directories computed for '{}': '{}'", this, targetDirectories);

    this.sourceDirectories = pathForPattern(output, SOURCE_PATTERN);
    LOGGER.debug("Source directories computed for '{}': '{}'", this, sourceDirectories);

    this.runtimeDir = pathForRuntimePattern(output);
    LOGGER.debug("Runtime computed for '{}': '{}'", this, runtimeDir);

    // if any of the above failed to get extracted, we throw an exception to indicate that
    //    if (fullProjectClassPath.isEmpty()
    //        || targetDirectories.isEmpty()
    //        || sourceDirectories.isEmpty()) {
    //      throw new BuildToolException(
    //          String.format(
    //              "Failed to retrieve one of the required build data.\nCP : %s\nTargetDirs:
    // %s\nSourceDirs: %s\n",
    //              fullProjectClassPath, targetDirectories, sourceDirectories));
    //    }

    this.initialized = true;
  }

  private Collection<String> pathForPattern(String output, Pattern pattern) {
    Collection<String> cp = new ArrayList<>();

    Matcher matcher = pattern.matcher(output);
    while (matcher.find()) {
      String match = matcher.group(2);
      cp.addAll(Arrays.asList(match.split(",")));
    }

    // the paths might not exist since they are not used and some of the tools can't handle this ->
    // remove them.
    cp =
        cp.stream()
            .map(String::trim)
            .distinct()
            .map(Paths::get)
            .filter(Files::exists)
            .map(Path::toString)
            .collect(Collectors.toList());

    return cp;
  }

  private Collection<String> pathForRuntimePattern(String output) {
    Collection<String> cp = new ArrayList<>();

    Matcher matcher = RUNTIME_PATTERN.matcher(output);
    while (matcher.find()) {
      String match = matcher.group(2);
      cp.addAll(Arrays.asList(match.split(",")));
    }

    // the paths might not exist since they are not used and some of the tools can't handle this ->
    // remove them.
    cp =
        cp.stream()
            .map(String::trim)
            // TODO only works for Java <= 8
            .distinct()
            .map(
                s ->
                    s
                        + (s.endsWith(File.separator) ? "" : File.separator)
                        + "lib"
                        + File.separator
                        + "rt.jar")
            .map(Paths::get)
            .filter(Files::exists)
            .map(Path::toString)
            .collect(Collectors.toList());

    return cp;
  }

  public Collection<String> getRuntimeDir() {
    return this.runtimeDir;
  }

  public static class BuildToolException extends Exception {
    public BuildToolException(String message) {
      super(message);
    }

    public BuildToolException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
