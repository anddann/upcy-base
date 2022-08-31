package de.upb.upcy.base.sigtest.worker.sigtest.checksource;

import com.google.common.base.Charsets;
import de.upb.upcy.base.commons.CompressionUtils;
import de.upb.upcy.base.sigtest.db.MongoDBHandler;
import de.upb.upcy.base.sigtest.db.model.check.SigTestCheckDBDoc;
import de.upb.upcy.base.sigtest.db.model.generate.SigTestDBDoc;
import de.upb.upcy.base.sigtest.worker.Utils;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.LoggerFactory;

public class Worker {

  private static final org.slf4j.Logger LOGGER =
      LoggerFactory.getLogger(de.upb.upcy.base.sigtest.worker.sigtest.check.Worker.class);

  private final MongoDBHandler mongoDBHandler;

  public Worker(MongoDBHandler mongoDBHandler) {
    this.mongoDBHandler = mongoDBHandler;
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
        compareVersion(cmpPair.getLeft(), cmpPair.getRight());
      }
    }
  }

  public void execute(Collection<SigTestDBDoc> sigTestDBDocs) {

    final Map<Pair<String, String>, Collection<Pair<SigTestDBDoc, SigTestDBDoc>>>
        pairCollectionMap = Utils.generateSemanticVersionPairsForComparison(sigTestDBDocs);

    for (Map.Entry<Pair<String, String>, Collection<Pair<SigTestDBDoc, SigTestDBDoc>>> entry :
        pairCollectionMap.entrySet()) {

      LOGGER.info(
          "Running comparision for {} : {}", entry.getKey().getLeft(), entry.getKey().getRight());

      for (Pair<SigTestDBDoc, SigTestDBDoc> cmpPair : entry.getValue()) {
        compareVersion(cmpPair.getLeft(), cmpPair.getRight());
      }
    }
  }

  private void compareVersion(SigTestDBDoc base, SigTestDBDoc nextVersion) {
    LOGGER.info("Compare {} : {}", base.getArtifactInfo(), nextVersion.getArtifactInfo());

    // we have to compare two version and get the report

    Path tempDirectory = null;
    try {
      tempDirectory = Files.createTempDirectory(RandomStringUtils.randomAlphabetic(10));

      Processor processor = new Processor(tempDirectory);
      final Path reportFile = processor.performAnalysis(base, nextVersion);

      if (reportFile == null) {
        throw new IOException("Signature Check Report Generation failed");
      }

      // take the file and store it in the Mongo DB including the statistics
      SigTestCheckDBDoc dbDocument = new SigTestCheckDBDoc();
      dbDocument.setBaseArtifact(base.getArtifactInfo());
      dbDocument.setNextArtifact(nextVersion.getArtifactInfo());

      // compute statistics of the comparision
      final int sigTestWarnings = this.getTotalNumOfErrors(reportFile);

      dbDocument.setNumberOfIncompatibleConstructs(sigTestWarnings);

      final String compressedContent =
          CompressionUtils.compressAndReturnB64(reportFile, Charsets.UTF_8);
      dbDocument.setReportFileContent(compressedContent);

      // write to the DB
      mongoDBHandler.addToDBSource(dbDocument);

    } catch (IOException ex) {
      LOGGER.error("Failed sigtest check", ex);
    }
  }

  private int getTotalNumOfErrors(Path reportFile) throws IOException {

    try (BufferedReader br = Files.newBufferedReader(reportFile)) {
      String st;
      while ((st = br.readLine()) != null) {
        final String prefix = "STATUS:Failed.";
        if (StringUtils.startsWith(st, prefix)) {
          // skip empty line
          final String substring = st.substring(prefix.length(), st.lastIndexOf(" "));
          return Integer.parseInt(substring);
        }
      }
    }
    return 0;
  }
}
