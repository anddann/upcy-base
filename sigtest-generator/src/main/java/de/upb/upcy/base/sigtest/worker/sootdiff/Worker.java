package de.upb.upcy.base.sigtest.worker.sootdiff;

import de.upb.upcy.base.sigtest.db.MongoDBHandler;
import de.upb.upcy.base.sigtest.db.model.generate.SigTestDBDoc;
import de.upb.upcy.base.sigtest.db.model.sootdiff.BasicAPICheckDoc;
import de.upb.upcy.base.sigtest.db.model.sootdiff.CallGraphCheckDoc;
import de.upb.upcy.base.sigtest.worker.Utils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.LoggerFactory;

public class Worker {

  private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Worker.class);

  private final MongoDBHandler mongoDBHandler;

  public Worker(MongoDBHandler mongoDBHandler) {
    this.mongoDBHandler = mongoDBHandler;
  }

  public void execute(Collection<SigTestDBDoc> sigTestDBDocs) {
    this.execute(
        sigTestDBDocs,
        sigTestDBDocs1 -> Utils.generateSemanticVersionPairsForComparison(sigTestDBDocs1));
  }

  public void execute(
      Collection<SigTestDBDoc> sigTestDBDocs,
      Function<
              Collection<SigTestDBDoc>,
              Map<Pair<String, String>, Collection<Pair<SigTestDBDoc, SigTestDBDoc>>>>
          sortFunction) {

    final Map<Pair<String, String>, Collection<Pair<SigTestDBDoc, SigTestDBDoc>>>
        pairCollectionMap = sortFunction.apply(sigTestDBDocs);

    for (Map.Entry<Pair<String, String>, Collection<Pair<SigTestDBDoc, SigTestDBDoc>>> entry :
        pairCollectionMap.entrySet()) {

      LOGGER.info(
          "Running comparison for {} : {}", entry.getKey().getLeft(), entry.getKey().getRight());

      for (Pair<SigTestDBDoc, SigTestDBDoc> cmpPair : entry.getValue()) {
        runComparision(cmpPair.getLeft(), cmpPair.getRight());
      }
    }
  }

  protected void runComparision(SigTestDBDoc baseVersion, SigTestDBDoc nextVersion) {
    LOGGER.info("Compare  {} : {}", baseVersion.getArtifactInfo(), nextVersion.getArtifactInfo());

    // we have to compare two version and get the report

    Path tempDirectory = null;
    try {
      tempDirectory = Files.createTempDirectory(RandomStringUtils.randomAlphabetic(10));

      Processor processor = new Processor(tempDirectory);
      final Pair<BasicAPICheckDoc, CallGraphCheckDoc> result =
          processor.performAnalysis(baseVersion, nextVersion);

      if (result == null) {
        throw new IOException("SootDiff Report Generation failed");
      }
      // write left result to MongoDB
      mongoDBHandler.addToDB(result.getLeft());

      // write right result to MongoDB
      mongoDBHandler.addToDB(result.getRight());

    } catch (IOException ex) {
      LOGGER.error("Failed SootDiff with", ex);
    }
  }
}
