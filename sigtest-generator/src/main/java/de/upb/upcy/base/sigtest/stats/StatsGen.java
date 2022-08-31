package de.upb.upcy.base.sigtest.stats;

import de.upb.upcy.base.sigtest.db.MongoDBHandler;
import de.upb.upcy.base.sigtest.db.model.check.SigTestCheckDBDoc;
import de.upb.upcy.base.sigtest.db.model.generate.SigTestDBDoc;
import de.upb.upcy.base.sigtest.db.model.sootdiff.BasicAPICheckDoc;
import de.upb.upcy.base.sigtest.db.model.sootdiff.CallGraphCheckDoc;
import de.upb.upcy.base.sigtest.worker.Utils;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.LoggerFactory;

public class StatsGen {

  private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(StatsGen.class);

  private final MongoDBHandler mongoDBHandler;

  private static final String STATS_DIR =
      System.getenv("STATS_DIR") == null ? "./stats" : System.getenv("STATS_DIR");

  public StatsGen(MongoDBHandler mongoDBHandler) {
    this.mongoDBHandler = mongoDBHandler;
  }

  public void execute(String groupId, String artifactId) {
    final Iterable<SigTestDBDoc> by = mongoDBHandler.findBy(groupId, artifactId);
    List<SigTestDBDoc> result = new ArrayList<SigTestDBDoc>();
    by.forEach(result::add);
    execute(result);
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

    genOverviewOfGeneratedSigs(sigTestDBDocs);

    final Map<Pair<String, String>, Collection<Pair<SigTestDBDoc, SigTestDBDoc>>>
        pairCollectionMap = sortFunction.apply(sigTestDBDocs);

    for (Map.Entry<Pair<String, String>, Collection<Pair<SigTestDBDoc, SigTestDBDoc>>> entry :
        pairCollectionMap.entrySet()) {
      LOGGER.info(
          "Generating Statistics for %s : %s", entry.getKey().getLeft(), entry.getKey().getRight());
      for (Pair<SigTestDBDoc, SigTestDBDoc> cmpPair : entry.getValue()) {
        genStats(cmpPair.getLeft(), cmpPair.getRight());
      }
    }
  }

  private void genOverviewOfGeneratedSigs(Collection<SigTestDBDoc> sigTestDBDocs) {
    final Optional<SigTestDBDoc> any = sigTestDBDocs.stream().findAny();
    if (!any.isPresent()) {
      return;
    }
    try (BufferedWriter writer =
        Files.newBufferedWriter(
            Paths.get(STATS_DIR)
                .resolve(
                    "sigtestgen_overview_" + any.get().getArtifactInfo().getGroupId() + ".csv"),
            StandardCharsets.UTF_8)) {

      String header = "groupId,artifactId,version";

      writer.write(header);
      writer.newLine();

      for (SigTestDBDoc sigTestDBDoc : sigTestDBDocs) {

        writer.write(sigTestDBDoc.getArtifactInfo().getGroupId());
        writer.write(",");
        writer.write(sigTestDBDoc.getArtifactInfo().getArtifactId());
        writer.write(",");
        writer.write(sigTestDBDoc.getArtifactInfo().getVersion());
        writer.newLine();
      }
      writer.newLine();

    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }

  private void genStats(SigTestDBDoc baseVersion, SigTestDBDoc nextVersion) {

    Path diffDir = Paths.get(StatsGen.STATS_DIR);
    String base =
        String.format(
            "%s_%s_%s_%s",
            baseVersion.getArtifactInfo().getGroupId(),
            baseVersion.getArtifactInfo().getArtifactId(),
            baseVersion.getArtifactInfo().getVersion(),
            baseVersion.getArtifactInfo().getClassifier());
    String next =
        String.format(
            "%s_%s_%s_%s",
            nextVersion.getArtifactInfo().getGroupId(),
            nextVersion.getArtifactInfo().getArtifactId(),
            nextVersion.getArtifactInfo().getVersion(),
            nextVersion.getArtifactInfo().getClassifier());

    String resultsFolder = base + "___" + next;

    Path resultFolderPath = diffDir.resolve(resultsFolder);
    if (!Files.exists(resultFolderPath)) {
      try {
        Files.createDirectory(resultFolderPath);
      } catch (IOException e) {
        LOGGER.error("Failed to create directory:" + resultFolderPath, e);
        return;
      }
    }

    LOGGER.info("Stats for {} : {}", baseVersion.getArtifactInfo(), nextVersion.getArtifactInfo());

    Iterable<SigTestCheckDBDoc> sigCheck = mongoDBHandler.findSigCheck(baseVersion, nextVersion);

    sigCheck = (Iterable<SigTestCheckDBDoc>) filterDuplicates(sigCheck);

    Iterable<SigTestCheckDBDoc> sigCheckSource =
        mongoDBHandler.findSigCheckSource(baseVersion, nextVersion);

    sigCheckSource = (Iterable<SigTestCheckDBDoc>) filterDuplicates(sigCheckSource);

    Iterable<BasicAPICheckDoc> sootDiffBasicAPICheck =
        mongoDBHandler.findSootDiffBasicAPICheck(baseVersion, nextVersion);

    sootDiffBasicAPICheck = (Iterable<BasicAPICheckDoc>) filterDuplicates(sootDiffBasicAPICheck);

    Iterable<CallGraphCheckDoc> sootDiffCGCheck =
        mongoDBHandler.findSootDiffCGCheck(baseVersion, nextVersion);

    sootDiffCGCheck = (Iterable<CallGraphCheckDoc>) filterDuplicates(sootDiffCGCheck);

    LOGGER.info("Gen PlainTexFile");

    PlainTextFile plainTextFile =
        new PlainTextFile(
            resultFolderPath, sigCheck, sigCheckSource, sootDiffBasicAPICheck, sootDiffCGCheck);
    plainTextFile.create(baseVersion, nextVersion);

    LOGGER.info("Gen CSVFile");

    CSVFile csvFile =
        new CSVFile(
            resultFolderPath, sigCheck, sigCheckSource, sootDiffBasicAPICheck, sootDiffCGCheck);
    csvFile.create(baseVersion, nextVersion);
  }

  private List<?> filterDuplicates(Iterable<?> iterable) {
    List<Object> result = new ArrayList<>();
    iterable.forEach(result::add);
    if (result.size() > 1) {
      LOGGER.warn("List contains duplicate documents");
      return Collections.singletonList(result.get(0));
    } else if (result.isEmpty()) {
      Collections.emptyList();
    }
    return result;
  }
}
