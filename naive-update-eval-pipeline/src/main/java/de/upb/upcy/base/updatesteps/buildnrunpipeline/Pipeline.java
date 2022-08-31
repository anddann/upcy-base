package de.upb.upcy.base.updatesteps.buildnrunpipeline;

import com.fasterxml.jackson.databind.JsonNode;
import de.upb.upcy.base.graph.GraphModel;
import de.upb.upcy.base.graph.GraphParser;
import de.upb.upcy.base.mvn.ConfigInstance;
import de.upb.upcy.base.mvn.MavenInvokerProject;
import de.upb.upcy.base.mvn.MavenSearchAPIClient;
import de.upb.upcy.base.updatesteps.PomModifier;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.plugin.surefire.log.api.NullConsoleLogger;
import org.apache.maven.plugins.surefire.report.ReportTestCase;
import org.apache.maven.plugins.surefire.report.ReportTestSuite;
import org.apache.maven.plugins.surefire.report.SurefireReportParser;
import org.apache.maven.reporting.MavenReportException;
import org.dom4j.DocumentException;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Pipeline {
  private static final Logger LOGGER = LoggerFactory.getLogger(Pipeline.class);
  private final Path rootPomFile;
  private final MavenInvokerProject mavenInvokerProject;
  private final Path orgPomFile;
  private final String projectName;
  private final Path modulePomFile;
  private final Consumer<Path> graphGeneratedCallback;
  private final List<Result> resultsList = new ArrayList<>();
  private DefaultDirectedGraph<GraphModel.Artifact, GraphModel.Dependency>
      artifactDependencyDefaultDirectedGraph;

  public Pipeline(
      Path rootProjectPomFile,
      Path modulePomFile,
      String projectName,
      @Nullable Consumer<Path> graphGeneratedCallback)
      throws IOException {
    this.rootPomFile = rootProjectPomFile;
    this.modulePomFile = modulePomFile;
    this.graphGeneratedCallback = graphGeneratedCallback;
    if (!Files.exists(rootPomFile)) {
      throw new IllegalArgumentException("Could not find pom file: " + rootProjectPomFile);
    }
    this.orgPomFile = modulePomFile.getParent().resolve("org_pom.xml");
    Files.copy(modulePomFile, orgPomFile, StandardCopyOption.REPLACE_EXISTING);

    this.mavenInvokerProject = new MavenInvokerProject(modulePomFile);
    this.projectName = projectName;
  }

  public List<Result> getResult() {
    return this.resultsList;
  }

  public DefaultDirectedGraph<GraphModel.Artifact, GraphModel.Dependency> generateGraph()
      throws IOException {

    MavenInvokerProject mavenInvokerProject = new MavenInvokerProject(modulePomFile);

    // get the dependency graph
    //  mvn com.github.ferstl:depgraph-maven-plugin:3.3.1:graph -DshowVersions -DshowGroupIds
    // -DshowDuplicates -DshowConflicts
    Triple<Integer, String, String> integerStringStringTriple;
    try {
      integerStringStringTriple =
          mavenInvokerProject.runCmd(
              "com.github.ferstl:depgraph-maven-plugin:4.0.1:graph",
              "-DshowVersions",
              "-DshowGroupIds",
              "-DshowDuplicates",
              "-DshowConflicts",
              "-DgraphFormat=json");

    } catch (MavenInvokerProject.BuildToolException e) {
      LOGGER.error("Failed to generate graph", e);
      throw new IOException("Could not generated graph: " + e.getMessage());
    }
    if (integerStringStringTriple.getLeft() != 0) {
      throw new IOException("Could not generated graph: " + integerStringStringTriple.getRight());
    }
    // get the file and build the graph model
    final Path jsonGraph = modulePomFile.getParent().resolve("target/dependency-graph.json");
    if (!Files.exists(jsonGraph)) {
      LOGGER.error("Could not find generated json graph output");
      return null;
    }

    if (this.graphGeneratedCallback != null) {
      this.graphGeneratedCallback.accept(jsonGraph);
    }

    final Pair<DefaultDirectedGraph<GraphModel.Artifact, GraphModel.Dependency>, GraphModel>
        pairGraph = GraphParser.parseGraph(jsonGraph);
    return pairGraph.getKey();
  }

  public void runPipeline() throws IOException {

    // build the graph
    this.artifactDependencyDefaultDirectedGraph = null;

    artifactDependencyDefaultDirectedGraph = generateGraph();

    if (artifactDependencyDefaultDirectedGraph == null) {
      LOGGER.error("Could not generate graph for project: {}", projectName);
      throw new IOException("Could not generate graph for project: " + projectName);
    }

    Result plainRun = new Result();
    plainRun.setProjectName(this.projectName);
    plainRun.setInDegree(-100);
    plainRun.setNewGav("");
    plainRun.setOrgGav("");
    plainRun.setTransitive(false);

    this.compileAndTest(plainRun);

    this.resultsList.add(plainRun);

    // stop if the initial build fails
    if (plainRun.getBuildResult() == Result.OUTCOME.FAIL) {
      LOGGER.error("Initial build failed: {}", this.projectName);
      return;
    }

    GraphModel.Artifact rootNode =
        artifactDependencyDefaultDirectedGraph.vertexSet().stream()
            .filter(x -> artifactDependencyDefaultDirectedGraph.inDegreeOf(x) == 0)
            .findFirst()
            .get();

    // iterate the following steps 25 times
    final Map<GraphModel.Artifact, List<JsonNode>> dependencyUpdates =
        this.genRandomUpdates(Collections.singletonList(rootNode), rootNode.getGroupId());

    if (dependencyUpdates.isEmpty()) {
      LOGGER.error("Found no updates in project: {}", this.projectName);
      Result depRun = new Result();
      depRun.setProjectName(this.projectName);
      depRun.setOrgGav("");
      depRun.setTransitive(false);
      depRun.setInDegree(-1);
      depRun.setNewGav("");
      depRun.setBuildResult(Result.OUTCOME.NO_UPDATES);
      depRun.setTestResult(Result.OUTCOME.SKIP);
      this.resultsList.add(depRun);
      // no need to continue for empty deps
      return;
    }

    // for each update create a report
    for (Map.Entry<GraphModel.Artifact, List<JsonNode>> entry : dependencyUpdates.entrySet()) {
      final GraphModel.Artifact art = entry.getKey();

      final GraphPath<GraphModel.Artifact, GraphModel.Dependency> pathBetween =
          DijkstraShortestPath.findPathBetween(
              artifactDependencyDefaultDirectedGraph, rootNode, art);
      final int i = artifactDependencyDefaultDirectedGraph.inDegreeOf(art);

      if (entry.getValue().isEmpty()) {
        Result depRun = new Result();
        depRun.setProjectName(this.projectName);
        depRun.setOrgGav(art.toGav());
        depRun.setTransitive(pathBetween.getLength() > 1);
        depRun.setInDegree(i);
        depRun.setNewGav("");
        depRun.setBuildResult(Result.OUTCOME.NO_UPDATES);
        depRun.setTestResult(Result.OUTCOME.SKIP);
        this.resultsList.add(depRun);
        // no need to continue for empty deps
        continue;
      }

      // restore the original pom file
      Files.copy(orgPomFile, modulePomFile, StandardCopyOption.REPLACE_EXISTING);
      final List<JsonNode> newerVersion = entry.getValue();

      // only select a certain number of newer versions, randomly

      final List<JsonNode> jsonUpdateNodes = new ArrayList<>();

      while (jsonUpdateNodes.size() < ConfigInstance.instance().getNumberOfLibsToUpdate()
          && !newerVersion.isEmpty()) {
        double random = Math.random();
        random = random * newerVersion.size();
        JsonNode randNewVersion = newerVersion.remove((int) random);
        // add to the choosen the vertex set
        jsonUpdateNodes.add(randNewVersion);
      }

      for (JsonNode newerVersionNode : jsonUpdateNodes) {

        Result depRun = new Result();
        depRun.setProjectName(this.projectName);
        depRun.setOrgGav(art.toGav());
        depRun.setTransitive(pathBetween.getLength() > 1);
        depRun.setInDegree(i);
        depRun.setNrOfNewerVersions(newerVersion.size());
        depRun.setNewGav(
            art.getGroupId()
                + ":"
                + art.getArtifactId()
                + ":"
                + newerVersionNode.get("v").asText());

        LOGGER.info("Update {} - to - {}", depRun.getOrgGav(), depRun.getNewGav());

        // update the pom and run (build, compile)
        // create a copy of the org
        Files.copy(orgPomFile, modulePomFile, StandardCopyOption.REPLACE_EXISTING);

        // update the pom file
        PomModifier pomModifier = new PomModifier(modulePomFile);
        try {
          pomModifier.addOrUpdate(
              art.getGroupId(), art.getArtifactId(), newerVersionNode.get("v").asText());
          this.compileAndTest(depRun);

        } catch (DocumentException e) {
          LOGGER.error("Failed to update pom.xml file", e);
          depRun.setTestResult(Result.OUTCOME.SKIP);
          depRun.setBuildResult(Result.OUTCOME.SKIP);
        } finally {

          this.resultsList.add(depRun);
        }
      }
    }

    // restore the org pom file
    Files.copy(orgPomFile, modulePomFile, StandardCopyOption.REPLACE_EXISTING);
  }

  private Map<GraphModel.Artifact, List<JsonNode>> genRandomUpdates(
      Collection<GraphModel.Artifact> excludeList, String ownGroupId) {
    HashMap<GraphModel.Artifact, List<JsonNode>> depUpdates = new HashMap<>();

    HashSet<GraphModel.Artifact> choosenDeps = new HashSet<>();
    // filter the root node, and libs with the same groupId (for aggregator projects)
    final List<GraphModel.Artifact> artifactList =
        artifactDependencyDefaultDirectedGraph.vertexSet().stream()
            .filter(x -> !excludeList.contains(x))
            .filter(x -> !StringUtils.equals(ownGroupId, x.getGroupId()))
            .filter(x -> x.getScopes().contains("compile"))
            .collect(Collectors.toList());
    // comment previous filter was && x.getScopes().size() == 1
    // however, by default some dependencies are compile and test... right?
    int numOfArtifact = ConfigInstance.instance().getNumberOfLibsToUpdate();
    while (choosenDeps.size() < numOfArtifact && !artifactList.isEmpty()) {
      double random = Math.random();
      random = random * artifactList.size();
      GraphModel.Artifact dependency = artifactList.remove((int) random);
      // add to the choosen the vertex set
      choosenDeps.add(dependency);
    }

    // check how many edges target this dependency (incl. conflict and duplicates)

    for (GraphModel.Artifact artifact : choosenDeps) {

      DefaultArtifactVersion defaultArtifactVersion =
          new DefaultArtifactVersion(artifact.getVersion());

      // number of artifacts that directly depends on this dependency
      // number of updates
      // check maven central for update version
      JsonNode listOfArtifacts = null;
      try {
        JsonNode response =
            MavenSearchAPIClient.getListOfArtifacts(
                artifact.getGroupId(), artifact.getArtifactId());
        listOfArtifacts = response.at("/response/docs");
      } catch (IOException e) {
        LOGGER.error("Failed to retrieve version from maven central", e);
      }
      if (listOfArtifacts == null || listOfArtifacts.isNull() || listOfArtifacts.isEmpty()) {
        LOGGER.error("Found no artifacts to update");
        return Collections.emptyMap();
      }
      ArrayList<JsonNode> newerVersion = new ArrayList<>();
      // get the ones with a newer version
      for (Iterator<JsonNode> iterator = listOfArtifacts.iterator(); iterator.hasNext(); ) {
        final JsonNode next = iterator.next();
        final DefaultArtifactVersion nextVersion =
            new DefaultArtifactVersion(next.get("v").asText());

        if (nextVersion.compareTo(defaultArtifactVersion) >= 1) {
          newerVersion.add(next);
        }
      }
      depUpdates.put(artifact, newerVersion);
    }
    return depUpdates;
  }

  public void compileAndTest(Result pipelineResults) {
    // 1. test if the normal build goal succeeds
    // 2. test if the normal test goal succeeds
    LOGGER.info("Building project: {}", projectName);
    try {
      Triple<Integer, String, String> out = mavenInvokerProject.compile();
      if (out.getLeft() == 0) {
        pipelineResults.setBuildResult(Result.OUTCOME.SUCCESS);
      } else {
        LOGGER.error("Building project failed");
        pipelineResults.setBuildResult(Result.OUTCOME.FAIL);
        pipelineResults.setTestResult(Result.OUTCOME.SKIP);
        pipelineResults.setBuildError(out.getRight());
      }
    } catch (MavenInvokerProject.BuildToolException e) {
      LOGGER.error("Building project failed", e);
      pipelineResults.setBuildResult(Result.OUTCOME.FAIL);
      return;
    }
    try {
      Triple<Integer, String, String> out = mavenInvokerProject.runCmd("test");
      parseTestResult(pipelineResults);
      if (out.getLeft() == 0) {
        pipelineResults.setTestResult(Result.OUTCOME.SUCCESS);
        LOGGER.error("Test Execution of projects failed");
      } else {
        pipelineResults.setTestResult(Result.OUTCOME.FAIL);
      }
    } catch (MavenInvokerProject.BuildToolException e) {
      LOGGER.error("Test Execution of projects failed", e);
      pipelineResults.setTestResult(Result.OUTCOME.FAIL);
    }
    LOGGER.info("Build and Test complete: {}", projectName);
  }

  public void parseTestResult(Result pipelineResults) {
    List<String> testErrors = new ArrayList<>();
    List<String> testFailures = new ArrayList<>();

    Path surefireReportsDirectory = getSurefireReportsDirectory();
    SurefireReportParser parser =
        new SurefireReportParser(
            Collections.singletonList(surefireReportsDirectory.toFile()),
            Locale.ENGLISH,
            new NullConsoleLogger());

    try {
      List<ReportTestSuite> testSuites = parser.parseXMLReportFiles();
      for (ReportTestSuite reportTestSuite : testSuites) {
        for (ReportTestCase testCase : reportTestSuite.getTestCases()) {
          if (testCase.hasError()) {
            testErrors.add(reportTestSuite.getFullClassName() + " : " + testCase.getFailureType());
          }
          if (testCase.hasFailure()) {
            testFailures.add(
                reportTestSuite.getFullClassName() + " : " + testCase.getFailureType());
          }
        }
      }
    } catch (MavenReportException e) {
      e.printStackTrace();
    }

    pipelineResults.setTestErrors(testErrors);

    pipelineResults.setTestFailures(testFailures);
  }

  private Path getSurefireReportsDirectory() {
    return modulePomFile.getParent().resolve("surefire-reports");
  }
}
