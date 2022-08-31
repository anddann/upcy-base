package de.upb.upcy.base.updatesteps.buildnrunpipeline;

import de.upb.upcy.base.build.Utils;
import de.upb.upcy.base.mvn.ConfigInstance;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.tuple.Pair;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiProjectRunner {

  private static final Logger LOGGER = LoggerFactory.getLogger(MultiProjectRunner.class);

  private final String projectsRootFolder;
  private final Path outputDir;

  public MultiProjectRunner(String projectsRootFolder, Path outputDir) {
    this.projectsRootFolder = projectsRootFolder;
    this.outputDir = outputDir;
  }

  private static void runOn(Path commitFile, Path outputDir) throws IOException {
    final String projectName =
        String.format(
            "%s_%s",
            commitFile.getParent().getParent().getFileName(), commitFile.getParent().getFileName());

    Path projectOutPut = outputDir.resolve(projectName);
    Files.createDirectories(projectOutPut);

    Path projectFolder = null;
    try {
      final Pair<String, String> repoAndCommit = Utils.getRepoAndCommit(commitFile);
      String repoUrl = repoAndCommit.getLeft();
      String commit = repoAndCommit.getRight();
      projectFolder = Utils.checkOutRepo(repoUrl, commit);
      final Path projectPom = projectFolder.resolve("pom.xml");
      Runner projectRun = new Runner(projectName, projectPom, projectOutPut);
      projectRun.run();
    } catch (GitAPIException e) {
      LOGGER.error("Failed to clone: " + projectName, e);
      try {
        Files.write(
            projectOutPut.resolve("failed_git.txt"),
            (projectName + " - " + e.getMessage()).getBytes(),
            StandardOpenOption.APPEND);
      } catch (IOException ee) {
        // exception handling left as an exercise for the reader
      }
    } catch (IOException e) {
      LOGGER.error("Failed to run project: " + projectName, e);
      Files.write(
          projectOutPut.resolve("failed_run.txt"),
          (projectName + " - " + e.getMessage()).getBytes(),
          StandardOpenOption.APPEND);
    } finally {
      if (projectFolder != null) {
        FileUtils.deleteDirectory(projectFolder.toFile());
      }
    }
  }

  public void run() throws IOException, InterruptedException {
    Set<Path> foundCommitFiles = Utils.findCommitFiles(this.projectsRootFolder);
    ExecutorService executorService =
        Executors.newFixedThreadPool(ConfigInstance.instance().getNumberOfThreads());
    List<Callable<Void>> tasks = new ArrayList<>();
    for (Path commitFile : foundCommitFiles) {
      Callable<Void> c =
          () -> {
            runOn(commitFile, outputDir);
            return null;
          };
      tasks.add(c);
    }

    List<Future<Void>> results = executorService.invokeAll(tasks);
    executorService.shutdown();
    try {
      if (!executorService.awaitTermination(800, TimeUnit.HOURS)) {
        executorService.shutdownNow();
      }
    } catch (InterruptedException e) {
      executorService.shutdownNow();
    }
  }
}
