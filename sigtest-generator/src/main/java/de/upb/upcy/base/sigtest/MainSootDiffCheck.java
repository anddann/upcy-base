package de.upb.upcy.base.sigtest;

import de.upb.upcy.base.sigtest.db.MongoDBHandler;
import de.upb.upcy.base.sigtest.db.model.generate.SigTestDBDoc;
import de.upb.upcy.base.sigtest.worker.Utils;
import de.upb.upcy.base.sigtest.worker.sootdiff.Worker;
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

public class MainSootDiffCheck {

  private final MongoDBHandler mongoDBHandler = MongoDBHandler.getInstance();

  // done commons-io commons-io

  public static void main(String[] args) throws IOException {
    if (args.length == 1) {
      // check if its a file
      final Path path = Paths.get(args[0]);
      if (!Files.exists(path)) {
        System.exit(-1);
      } else {
        MainSootDiffCheck mainSootDiffCheck = new MainSootDiffCheck();
        final List<String> lines = Files.readAllLines(path);
        for (String line : lines) {
          String[] split = line.split(",");
          if (split.length != 2) {
            continue;
          }
          mainSootDiffCheck.handle(split[0].trim(), split[1].trim());
        }
      }
    } else if (args.length == 2) {
      String groupId = args[0];
      String artifactId = args[1];
      MainSootDiffCheck mainSootDiffCheck = new MainSootDiffCheck();
      mainSootDiffCheck.handle(groupId, artifactId);

    } else {
      System.err.println("No Program arguments given");
      System.exit(-1);
    }
  }

  public void handle(String groupId, String artifactId) throws IOException {
    this.handle(
        groupId,
        artifactId,
        sigTestDBDocs -> Utils.generateSemanticVersionPairsForComparison(sigTestDBDocs));
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
