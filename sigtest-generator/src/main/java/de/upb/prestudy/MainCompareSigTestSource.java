package de.upb.prestudy;

import de.upb.prestudy.db.MongoDBHandler;
import de.upb.prestudy.db.model.generate.SigTestDBDoc;
import de.upb.prestudy.worker.sigtest.checksource.Worker;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.apache.commons.lang3.tuple.Pair;

public class MainCompareSigTestSource {
  private final MongoDBHandler mongoDBHandler = MongoDBHandler.getInstance();

  // done commons-io commons-io

  public static void main(String[] args) throws IOException {
    if (args.length == 1) {
      // check if its a file
      final Path path = Paths.get(args[0]);
      if (!Files.exists(path)) {
        System.exit(-1);
      } else {
        MainCompareSigTestSource mainGenerateSigTest = new MainCompareSigTestSource();
        final List<String> lines = Files.readAllLines(path);
        for (String line : lines) {
          String[] split = line.split(",");
          if (split.length != 2) {
            continue;
          }
          mainGenerateSigTest.handle(split[0].trim(), split[1].trim());
        }
      }
    } else if (args.length == 2) {
      String groupId = args[0];
      String artifactId = args[1];
      MainCompareSigTestSource mainCompareSigTest = new MainCompareSigTestSource();
      mainCompareSigTest.handle(groupId, artifactId);

    } else {
      System.exit(-1);
    }
  }

  private void handle(String groupId, String artifactId) throws IOException {
    // compare for each group, artifactId the sigtest results for binary incompability
    // get the gavs from the DB
    final Iterable<SigTestDBDoc> by = mongoDBHandler.findBy(groupId, artifactId);
    Worker worker = new Worker(mongoDBHandler);
    List<SigTestDBDoc> result = new ArrayList<SigTestDBDoc>();
    by.forEach(result::add);
    worker.execute(result);
  }

  public void handle(
      String groupId,
      String artifactId,
      Function<
              Collection<SigTestDBDoc>,
              Map<Pair<String, String>, Collection<Pair<SigTestDBDoc, SigTestDBDoc>>>>
          compareFunction)
      throws IOException {
    // compare for each group, artifactId the sigtest results for binary incompability
    // get the gavs from the DB
    final Iterable<SigTestDBDoc> by = mongoDBHandler.findBy(groupId, artifactId);
    Worker worker = new Worker(mongoDBHandler);
    List<SigTestDBDoc> result = new ArrayList<SigTestDBDoc>();
    by.forEach(result::add);
    worker.execute(result, compareFunction);
  }
}
