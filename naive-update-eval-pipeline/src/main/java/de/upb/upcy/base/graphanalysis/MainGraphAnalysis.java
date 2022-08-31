package de.upb.upcy.base.graphanalysis;

import static java.util.stream.Collectors.groupingBy;

import com.opencsv.CSVWriter;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import de.upb.upcy.base.updatesteps.buildnrunpipeline.Result;
import de.upb.upcy.base.updatesteps.buildnrunpipeline.Runner;
import de.upb.upcy.base.updatesteps.graphanalyzer.GraphAnalyzer;
import de.upb.upcy.base.updatesteps.graphanalyzer.GraphResult;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainGraphAnalysis {

  private static final Logger LOGGER = LoggerFactory.getLogger(Runner.class);

  public static void main(String[] args) throws IOException {

    String rootDir = args[0];
    // get the csv file to work on
    AtomicReference<Path> csvFile = new AtomicReference<>();
    List<Path> graphFiles = new ArrayList<>();
    try (Stream<Path> walkStream = Files.walk(Paths.get(rootDir))) {
      walkStream
          .filter(p -> p.toFile().isFile())
          .forEach(
              f -> {
                if (StringUtils.endsWith(f.getFileName().toString(), ".csv")
                    && !StringUtils.contains(f.getFileName().toString(), "graph-analysis")) {
                  // ignore graph analysis csv file
                  csvFile.set(f);
                }
                if (StringUtils.endsWith(f.getFileName().toString(), "dependency-graph.json")) {
                  graphFiles.add(f);
                }
              });
    }

    try {
      run(csvFile.get(), graphFiles);
    } catch (IOException e) {
      LOGGER.error("Failed to run analysis on:", e);
    }
  }

  public static void run(Path csvFile, List<Path> jsonGraphFiles) throws IOException {

    LOGGER.info("Start on project: {}", csvFile.toString());

    Map<String, Path> projectNameToGraphFile =
        jsonGraphFiles.stream()
            .collect(
                Collectors.toMap((args) -> args.getFileName().toString(), Function.identity()));

    List<Result> results;
    try (Reader reader = Files.newBufferedReader(csvFile)) {
      CsvToBean sbc =
          new CsvToBeanBuilder(reader)
              .withType(Result.class)
              .withSeparator(CSVWriter.DEFAULT_SEPARATOR)
              .build();
      results = sbc.parse();
    }

    // get project name
    // collect the modified gav
    Map<String, List<Result>> resultPerProject =
        results.stream().collect(groupingBy(Result::getProjectName));

    List<GraphResult> graphResults = new ArrayList<>();

    for (Map.Entry<String, List<Result>> entry : resultPerProject.entrySet()) {

      final Set<String> gavs =
          entry.getValue().stream()
              .map(Result::getOrgGav)
              .filter(x -> StringUtils.isNotBlank(x))
              .collect(Collectors.toSet());

      String projectName = entry.getKey();
      // get the graph file

      Path jsonGraphFile = projectNameToGraphFile.get(projectName + "_dependency-graph.json");
      if (jsonGraphFile == null || !Files.exists(jsonGraphFile)) {
        LOGGER.error("Could not find json graph file: {}", jsonGraphFile);
        continue;
      }

      for (String gav : gavs) {
        GraphAnalyzer graphAnalyzer = new GraphAnalyzer(projectName, gav, jsonGraphFile.toString());
        final GraphResult analyze = graphAnalyzer.analyze();
        graphResults.add(analyze);
      }
    }

    // save to file
    try {
      final Path outputDir = csvFile.getParent();
      String fileName = csvFile.getFileName().toString();
      CSVWriter writer =
          new CSVWriter(
              new FileWriter(
                  outputDir.resolve(fileName.replace(".csv", "_graph-analysis.csv")).toFile()));
      StatefulBeanToCsv sbc =
          new StatefulBeanToCsvBuilder(writer).withSeparator(CSVWriter.DEFAULT_SEPARATOR).build();
      sbc.write(graphResults);
      writer.close();
    } catch (CsvRequiredFieldEmptyException | CsvDataTypeMismatchException | IOException e) {
      e.printStackTrace();
    }
    LOGGER.info("Done on project: {}", csvFile);
  }
}
