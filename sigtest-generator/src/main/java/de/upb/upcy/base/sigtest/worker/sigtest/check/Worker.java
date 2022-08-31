package de.upb.upcy.base.sigtest.worker.sigtest.check;

import com.google.common.base.Charsets;
import de.upb.upcy.base.commons.CompressionUtils;
import de.upb.upcy.base.sigtest.db.MongoDBHandler;
import de.upb.upcy.base.sigtest.db.model.check.SigTestCheckDBDoc;
import de.upb.upcy.base.sigtest.db.model.check.SigTestWarning;
import de.upb.upcy.base.sigtest.db.model.generate.SigTestDBDoc;
import de.upb.upcy.base.sigtest.worker.Utils;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
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
      final List<SigTestWarning> sigTestWarnings = this.parseReportFile(reportFile);

      // compute the number of classes
      final long count = sigTestWarnings.stream().count();
      dbDocument.setNumberOfIncompatibleClasses((int) count);
      // num of incomp constructs

      dbDocument.setNumberOfIncompatibleConstructs(getTotalNumOfErrors(reportFile));

      final String compressedContent =
          CompressionUtils.compressAndReturnB64(reportFile, Charsets.UTF_8);
      dbDocument.setReportFileContent(compressedContent);

      // write to the DB
      mongoDBHandler.addToDB(dbDocument);

    } catch (IOException ex) {
      LOGGER.error("Failed sigtest check", ex);
    }
  }

  private List<SigTestWarning> parseReportFile(Path reportFile) throws IOException {

    List<SigTestWarning> sigTestWarnings = new ArrayList<>();
    SigTestWarning current = null;
    try (BufferedReader br = Files.newBufferedReader(reportFile)) {
      for (int i = 0; i < 5; i++) {
        // skip the header
        br.readLine();
      }
      String st;
      while ((st = br.readLine()) != null) {
        if (StringUtils.isBlank(st) || StringUtils.isAllEmpty(st)) {
          // skip empty line
          continue;
        } else if (StringUtils.startsWith(st, "Class")) {
          // we found a new class
          final String[] s = st.split(" ");
          current = new SigTestWarning();
          current.setFqClassName(s[1]);
          sigTestWarnings.add(current);

        } else if (!StringUtils.startsWith(st, "Class") && current == null) {
          continue; // skip warnings, before the class part started
        } else if (StringUtils.isNotBlank(st)) {
          current.getWarnings().add(st);
        }
      }
    }
    return sigTestWarnings;
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
