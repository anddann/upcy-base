package de.upb.upcy.base.mvn;

import com.google.common.base.Preconditions;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IOUtils {
  public static final int TIMEOUT_EXITCODE = 124;
  private static final Logger LOGGER = LoggerFactory.getLogger(IOUtils.class);

  /**
   * Makes sure to consume error/input stream of the process so it can properly terminate.
   *
   * @param process
   * @param timeOutInSeconds -1 if none
   * @return
   */
  public static Triple<Integer, String, String> awaitTermination(
      Process process, int timeOutInSeconds) throws ExecutionException, InterruptedException {

    Preconditions.checkArgument(process.isAlive());

    CompletableFuture<String> soutFut = readOutStream("out", process.getInputStream());
    CompletableFuture<String> serrFut = readOutStream("err", process.getErrorStream());

    if (timeOutInSeconds <= 0) {
      process.waitFor();
    } else {
      process.waitFor(timeOutInSeconds, TimeUnit.SECONDS);
    }

    return Triple.of(
        process.isAlive() ? TIMEOUT_EXITCODE : process.exitValue(), soutFut.get(), serrFut.get());
  }

  static CompletableFuture<String> readOutStream(String prefix, InputStream is) {
    return CompletableFuture.supplyAsync(
        () -> {
          try (InputStreamReader isr = new InputStreamReader(is);
              BufferedReader br = new BufferedReader(isr)) {
            StringBuilder res = new StringBuilder();
            String inputLine;
            while ((inputLine = br.readLine()) != null) {
              res.append(inputLine).append(System.lineSeparator());
              LOGGER.trace("{}: {}", prefix, inputLine);
            }
            return res.toString();
          } catch (Throwable e) {
            throw new RuntimeException("problem with executing program", e);
          }
        });
  }

  private static Path getOutPath(String resourceName, Path outDir) {
    resourceName = toRelativeResourcePath(resourceName);

    return outDir.resolve(resourceName);
  }

  private static String toRelativeResourcePath(String resourceName) {
    if (resourceName.startsWith("/")) {
      resourceName = resourceName.substring(1);
    }
    return resourceName;
  }

  public static void deleteDirectory(Path dir) throws IOException {
    if (!Files.exists(dir)) {
      return;
    }

    Files.walk(dir)
        .sorted(Comparator.reverseOrder())
        .forEach(
            path -> {
              try {
                Files.deleteIfExists(path);
              } catch (IOException e) {
                e.printStackTrace();
              }
            });
  }
}
