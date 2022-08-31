package de.upb.prestudy;

import de.upb.prestudy.db.MongoDBHandler;
import de.upb.prestudy.db.model.generate.SigTestDBDoc;
import de.upb.prestudy.stats.StatsGen;
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

public class MainGetStatistics {

  private final MongoDBHandler mongoDBHandler = MongoDBHandler.getInstance();

  public static void main(String[] args) throws IOException {
    if (args.length == 1) {
      // check if its a file
      final Path path = Paths.get(args[0]);
      if (!Files.exists(path)) {
        System.exit(-1);
      } else {
        MainGetStatistics mainGetStatistics = new MainGetStatistics();
        final List<String> lines = Files.readAllLines(path);
        for (String line : lines) {
          String[] split = line.split(",");
          if (split.length != 2) {
            continue;
          }
          mainGetStatistics.handle(split[0].trim(), split[1].trim());
        }
      }
    } else if (args.length == 2) {
      String groupId = args[0];
      String artifactId = args[1];
      MainGetStatistics mainGenerateSigTest = new MainGetStatistics();
      mainGenerateSigTest.handle(groupId, artifactId);

    } else {
      System.exit(-1);
    }
  }

  private void handle(String groupId, String artifactId) throws IOException {
    System.out.println("Stats for " + groupId + ":" + artifactId);
    StatsGen statsGen = new StatsGen(mongoDBHandler);
    statsGen.execute(groupId, artifactId);
  }

  public void handle(
      String groupId,
      String artifactId,
      Function<
              Collection<SigTestDBDoc>,
              Map<Pair<String, String>, Collection<Pair<SigTestDBDoc, SigTestDBDoc>>>>
          compareFunction)
      throws IOException {
    // get the gavs from the DB
    List<SigTestDBDoc> result = new ArrayList<SigTestDBDoc>();

    final Iterable<SigTestDBDoc> by = mongoDBHandler.findBy(groupId, artifactId);
    by.forEach(result::add);
    StatsGen statsGen = new StatsGen(mongoDBHandler);
    statsGen.execute(result, compareFunction);
  }
}
