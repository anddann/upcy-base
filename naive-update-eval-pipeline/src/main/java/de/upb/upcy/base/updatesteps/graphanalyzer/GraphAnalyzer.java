package de.upb.upcy.base.updatesteps.graphanalyzer;

import de.upb.upcy.base.graph.GraphModel;
import de.upb.upcy.base.graph.GraphParser;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jgrapht.graph.DefaultDirectedGraph;

public class GraphAnalyzer {

  private final String projectName;
  private final String orgGav;
  private final Path jsonGraphFile;

  public GraphAnalyzer(String projectName, String orgGav, String graphFile) {

    this.projectName = projectName;
    this.orgGav = orgGav;
    this.jsonGraphFile = Paths.get(graphFile);
  }

  public GraphResult analyze() throws IOException {
    GraphResult graphResult = new GraphResult();
    graphResult.setProjectName(this.projectName);
    graphResult.setOrgGav(this.orgGav);

    // get the graph
    final Pair<DefaultDirectedGraph<GraphModel.Artifact, GraphModel.Dependency>, GraphModel>
        pairGraph = GraphParser.parseGraph(this.jsonGraphFile);
    final DefaultDirectedGraph<GraphModel.Artifact, GraphModel.Dependency>
        dependencyDefaultDirectedGraph = pairGraph.getLeft();

    // get the node
    final Optional<GraphModel.Artifact> first =
        dependencyDefaultDirectedGraph.vertexSet().stream()
            .filter(x -> StringUtils.equals(x.toGav(), orgGav))
            .findFirst();
    final GraphModel.Artifact artifactNode =
        first.orElseThrow(
            () -> new IllegalArgumentException("Could not find node in graph: " + orgGav));

    final Set<GraphModel.Dependency> inComingEdges =
        dependencyDefaultDirectedGraph.incomingEdgesOf(artifactNode);

    // compute conflicts
    ArrayList<String> conflicts = new ArrayList<>();
    final List<GraphModel.Dependency> conflictEdges =
        inComingEdges.stream()
            .filter(x -> x.getResolution() == GraphModel.ResolutionType.OMITTED_FOR_CONFLICT)
            .collect(Collectors.toList());

    for (GraphModel.Dependency dependencyEdge : conflictEdges) {
      // source Node
      final String format =
          String.format("%s --> %s", dependencyEdge.getFrom(), dependencyEdge.getTo());
      conflicts.add(format);
    }
    graphResult.setConflicts(conflicts);

    // compute duplicates
    final List<GraphModel.Dependency> dupEdges =
        inComingEdges.stream()
            .filter(x -> x.getResolution() == GraphModel.ResolutionType.OMITTED_FOR_DUPLICATE)
            .collect(Collectors.toList());
    ArrayList<String> duplicates = new ArrayList<>();
    for (GraphModel.Dependency dependencyEdge : dupEdges) {
      // source Node
      final String format =
          String.format("%s --> %s", dependencyEdge.getFrom(), dependencyEdge.getTo());
      duplicates.add(format);
    }
    graphResult.setDuplicates(duplicates);

    // get the root nodes
    final List<GraphModel.Artifact> rootNodes =
        dependencyDefaultDirectedGraph.vertexSet().stream()
            .filter(x -> dependencyDefaultDirectedGraph.inDegreeOf(x) == 0)
            .collect(Collectors.toList());

    List<GraphModel.Artifact> sourceNodes = new ArrayList<>();
    for (GraphModel.Dependency depEdge : inComingEdges) {
      final GraphModel.Artifact edgeSource = dependencyDefaultDirectedGraph.getEdgeSource(depEdge);
      sourceNodes.add(edgeSource);
    }

    // compute source-code
    final List<String> collectSrc =
        sourceNodes.stream()
            .filter(rootNodes::contains)
            .map(GraphModel.Artifact::toGav)
            .collect(Collectors.toList());
    graphResult.setSrcDepending(collectSrc);

    // compute ABI constraints
    final List<String> collectABI =
        sourceNodes.stream()
            .filter(x -> !rootNodes.contains(x))
            .map(GraphModel.Artifact::toGav)
            .collect(Collectors.toList());
    graphResult.setAbiDepending(collectABI);

    // compute blossom - size
    final List<GraphModel.Artifact> blossomNodes =
        dependencyDefaultDirectedGraph.vertexSet().stream()
            .filter(
                x ->
                    (StringUtils.equals(x.getGroupId(), artifactNode.getGroupId())
                        && x != artifactNode))
            .collect(Collectors.toList());
    graphResult.setBlossom(
        blossomNodes.stream().map(GraphModel.Artifact::toGav).collect(Collectors.toList()));

    return graphResult;
  }
}
