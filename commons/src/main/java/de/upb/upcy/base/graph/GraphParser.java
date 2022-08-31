package de.upb.upcy.base.graph;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;
import org.apache.commons.lang3.tuple.Pair;
import org.jgrapht.graph.DefaultDirectedGraph;

public class GraphParser {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public static Pair<DefaultDirectedGraph<GraphModel.Artifact, GraphModel.Dependency>, GraphModel>
      parseGraph(Path jsonGraph) throws IOException {
    final GraphModel graphModel = OBJECT_MAPPER.readValue(jsonGraph.toFile(), GraphModel.class);

    // build jgrapht
    final DefaultDirectedGraph defaultDirectedGraph =
        new DefaultDirectedGraph<GraphModel.Artifact, GraphModel.Dependency>(
            GraphModel.Dependency.class);

    for (GraphModel.Artifact artifact : graphModel.getArtifacts()) {
      defaultDirectedGraph.addVertex(artifact);
    }

    for (GraphModel.Dependency dependency : graphModel.getDependencies()) {
      // get source vertex
      GraphModel.Artifact source = graphModel.getArtifacts().get(dependency.getNumericFrom());
      // get target verext
      GraphModel.Artifact target = graphModel.getArtifacts().get(dependency.getNumericTo());

      defaultDirectedGraph.addEdge(source, target, dependency);
    }

    return Pair.of(defaultDirectedGraph, graphModel);
  }
}
