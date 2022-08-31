package de.upb.upcy.base.build;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utils {
  private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);

  public static Pair<String, String> getRepoAndCommit(Path commitFile) throws IOException {
    String repoUrl =
        String.format(
            "https://github.com/%s/%s.git",
            commitFile.getParent().getParent().getFileName(), commitFile.getParent().getFileName());
    // read the commitString from the file;
    String line = Files.lines(commitFile).findFirst().get().trim();
    return Pair.of(repoUrl, line);
  }

  public static Path checkOutRepo(String repoUrl, String commit)
      throws IOException, GitAPIException {

    final Path tmpDir = Files.createTempDirectory("projectRun");

    LOGGER.info("Cloning " + repoUrl + " into " + tmpDir);
    final Git git = Git.cloneRepository().setURI(repoUrl).setDirectory(tmpDir.toFile()).call();

    LOGGER.info("Check out commit: {}", commit);

    git.checkout().setName(commit).setForce(true).call();
    LOGGER.info("Completed Cloning");

    return tmpDir;
  }

  public static Set<Path> findCommitFiles(String projectsRootFolder) throws IOException {
    Set<Path> foundCommitFiles = new HashSet<>();
    try (Stream<Path> walkStream = Files.walk(Paths.get(projectsRootFolder))) {
      walkStream
          .filter(p -> p.toFile().isFile())
          .forEach(
              f -> {
                if (f.getFileName().toString().equals("COMMIT")) {
                  foundCommitFiles.add(f);
                }
              });
    }

    return foundCommitFiles;
  }
}
