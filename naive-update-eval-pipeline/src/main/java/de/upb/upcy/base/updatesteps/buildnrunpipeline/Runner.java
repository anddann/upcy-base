package de.upb.upcy.base.updatesteps.buildnrunpipeline;

import com.opencsv.CSVWriter;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import de.upb.upcy.base.mvn.MavenInvokerProject;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Runner {
  private static final Logger LOGGER = LoggerFactory.getLogger(Runner.class);
  private final String projectName;
  private final Path projectPomFile;
  private final Path outputDir;
  private final ExecutorService executorService;

  public Runner(String projectName, Path projectPomFile, Path outputDir) {

    this.projectName = projectName;
    this.projectPomFile = projectPomFile;
    this.outputDir = outputDir;
    executorService = Executors.newFixedThreadPool(4);
  }

  public void run() {
    if (!Files.exists(projectPomFile)) {
      LOGGER.error("Could not find pom file: {}", projectPomFile.toAbsolutePath());
    }

    LOGGER.info("Working on project: {}", projectPomFile);

    // run mvn compile install, to ease graph generation for aggregator projects
    // mvn clean compile install -DskipTests -Dmaven.test.skip=true
    MavenInvokerProject mavenInvokerProject = new MavenInvokerProject(projectPomFile);

    try {
      Triple<Integer, String, String> integerStringStringTriple =
          mavenInvokerProject.runCmd(
              "clean", "compile", "install", "-DskipTests", "-Dmaven.test.skip=true");

      if (integerStringStringTriple.getLeft() != 0) {
        throw new MavenInvokerProject.BuildToolException(integerStringStringTriple.getRight());
      }
      LOGGER.info("Successfully build initial with clean compile install");

    } catch (MavenInvokerProject.BuildToolException e) {
      LOGGER.error("Could not build pom file: {}", projectPomFile.toAbsolutePath());

      String msg =
          "Failed project compile and install : " + projectName + " with " + e.getMessage();

      try {
        Files.write(
            outputDir.resolve(projectName + "_error.txt"), msg.getBytes(StandardCharsets.UTF_8));
        return;
      } catch (IOException ex) {
        ex.printStackTrace();
      }
    }

    // must be of type list, to allow proper writing in csv file, e.g., collection does not work
    final List<Result> aggResults = new ArrayList<>();
    Collection<Callable<List<Result>>> tasks = new ArrayList<>();
    Collection<Callable<List<Result>>> rootProjectCallable = new ArrayList<>();
    // check for multi-module aka aggregator maven projects
    try (Stream<Path> walkStream = Files.walk(projectPomFile.getParent())) {
      walkStream
          .filter(p -> p.toFile().isFile())
          .forEach(
              f -> {
                if (StringUtils.equals(f.getFileName().toString(), "pom.xml")) {
                  LOGGER.info("Create Callable for {}", f.toAbsolutePath());

                  boolean sameFile;
                  try {
                    sameFile = Files.isSameFile(f, projectPomFile);
                  } catch (IOException e) {
                    sameFile = false;
                  }
                  if (sameFile) {
                    // the root project pom should not interfere with the paralleized builds
                    rootProjectCallable.add(
                        new Callable<List<Result>>() {
                          @Override
                          public List<Result> call() throws Exception {
                            return runOnSubmodule(f);
                          }
                        });

                  } else {
                    tasks.add(
                        new Callable<List<Result>>() {
                          @Override
                          public List<Result> call() throws Exception {
                            return runOnSubmodule(f);
                          }
                        });
                  }
                }
              });
    } catch (IOException exception) {
      LOGGER.error("Failed iterating dir ", exception);
    }

    List<Future<List<Result>>> futures = null;
    try {
      LOGGER.info("Found #{} projects to build", tasks.size());
      futures = executorService.invokeAll(tasks);
      for (Future<List<Result>> future : futures) {
        try {
          List<Result> x = future.get();
          aggResults.addAll(x);
        } catch (InterruptedException | ExecutionException e) {
          LOGGER.error("Failed task submission with: ", e);
        }
      }

      // now invoke the root project pom
      futures = executorService.invokeAll(rootProjectCallable);
      for (Future<List<Result>> future : futures) {
        try {
          List<Result> x = future.get();
          aggResults.addAll(x);

        } catch (InterruptedException | ExecutionException e) {
          LOGGER.error("Failed task submission with: ", e);
        }
      }

      executorService.shutdown();
    } catch (InterruptedException e) {
      LOGGER.error("Failed task submission with: ", e);
    }

    try {
      CSVWriter writer =
          new CSVWriter(new FileWriter(outputDir.resolve(projectName + ".csv").toFile()));
      StatefulBeanToCsv sbc =
          new StatefulBeanToCsvBuilder(writer).withSeparator(CSVWriter.DEFAULT_SEPARATOR).build();

      sbc.write(aggResults);
      writer.close();
    } catch (CsvRequiredFieldEmptyException | CsvDataTypeMismatchException | IOException e) {
      LOGGER.error("Failed to write csv file with: ", e);
    }
    LOGGER.info("Done on project: {}", projectPomFile);
  }

  private List<Result> runOnSubmodule(Path f) throws IOException {
    String newProjectName = projectName;
    if (!Files.isSameFile(projectPomFile, f)) {
      // we have a pom in a submodule
      newProjectName = projectName + "_" + f.getParent().getFileName().toString();
    }
    LOGGER.info("Running on file: {}, with projectName: {}", f.toAbsolutePath(), newProjectName);

    try {

      String finalNewProjectName = newProjectName;
      Consumer<Path> generatedGraphCallback =
          new Consumer<Path>() {
            @Override
            public void accept(Path path) {
              // save the graph
              try {
                Files.copy(path, outputDir.resolve(finalNewProjectName + "_dependency-graph.json"));
              } catch (IOException e) {
                LOGGER.error("Failed to copy graph with: ", e);
              }
            }
          };

      Pipeline projectPipeline =
          new Pipeline(projectPomFile, f, newProjectName, generatedGraphCallback);

      projectPipeline.runPipeline();
      final List<Result> result = projectPipeline.getResult();

      return result;
    } catch (IOException e) {

      String msg = "Failed project: " + projectName + " with " + e.getMessage();

      try {
        Files.write(
            outputDir.resolve(projectName + "_error.txt"), msg.getBytes(StandardCharsets.UTF_8));
      } catch (IOException ex) {
        ex.printStackTrace();
      }

      LOGGER.error("Failed for {} , {}", f.toAbsolutePath(), newProjectName);
    }
    return Collections.emptyList();
  }
}
