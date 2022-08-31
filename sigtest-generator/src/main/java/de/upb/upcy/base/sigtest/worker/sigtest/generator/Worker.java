package de.upb.upcy.base.sigtest.worker.sigtest.generator;

import com.google.common.base.Charsets;
import com.sun.tdk.signaturetest.model.ClassDescription;
import de.upb.upcy.base.commons.ArtifactInfo;
import de.upb.upcy.base.commons.CompressionUtils;
import de.upb.upcy.base.sigtest.db.MongoDBHandler;
import de.upb.upcy.base.sigtest.db.model.ModelTransformer;
import de.upb.upcy.base.sigtest.db.model.SimpleClassDescriptor;
import de.upb.upcy.base.sigtest.db.model.generate.SigTestDBDoc;
import de.upb.upcy.base.sigtest.sigfileutils.SigFileParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

public class Worker {

  private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Worker.class);

  private final MongoDBHandler mongoDBHandler;

  public Worker(MongoDBHandler mongoDBHandler) {
    this.mongoDBHandler = mongoDBHandler;
  }

  public void execute(Collection<ArtifactInfo> artifactInfoCollection) {

    artifactInfoCollection.forEach(this::execute);
  }

  public void execute(ArtifactInfo artifactInfo) {

    LOGGER.info(
        "Start Analysis of: "
            + artifactInfo.getGroupId()
            + ":"
            + artifactInfo.getArtifactId()
            + ":"
            + artifactInfo.getVersion());

    //    if (!StringUtils.equalsIgnoreCase(artifactInfo.getP(),"jar")) {
    //      // no jar file
    //      LOGGER.info("Skipping non jar file " + artifactInfo.getP());
    //      return;
    //    }

    if (StringUtils.contains(artifactInfo.getVersion(), "android")) {
      LOGGER.info(
          "Skipping artifact: "
              + artifactInfo.getGroupId()
              + ":"
              + artifactInfo.getArtifactId()
              + ":"
              + artifactInfo.getVersion());
      return;
    }

    // check if artifact already in db
    final SigTestDBDoc by =
        mongoDBHandler.findBy(
            artifactInfo.getGroupId(), artifactInfo.getArtifactId(), artifactInfo.getVersion());
    if (by != null) {
      LOGGER.info(
          "Already in DB - Skipping artifact: "
              + artifactInfo.getGroupId()
              + ":"
              + artifactInfo.getArtifactId()
              + ":"
              + artifactInfo.getVersion());
      return;
    }

    Path tempDirectory = null;
    try {
      tempDirectory = Files.createTempDirectory(RandomStringUtils.randomAlphabetic(10));

      // create the sigtest file
      Processor processor = new Processor(tempDirectory);

      final Path sigTestFile = processor.performAnalysis(artifactInfo);
      if (sigTestFile == null) {
        throw new IOException("Download / SigFile Generation failed");
      }
      LOGGER.info("Generated sigtest file {} successfully", sigTestFile.getFileName().toString());

      // take the file and store it in the Mongo DB including the statistics
      SigTestDBDoc dbDocument = new SigTestDBDoc();
      dbDocument.setArtifactInfo(artifactInfo);

      final String s = CompressionUtils.compressAndReturnB64(sigTestFile, Charsets.UTF_8);
      dbDocument.setSigTestFileContent(s);

      LOGGER.info("Start Parsing sigtest file {}", sigTestFile.getFileName().toString());

      SigFileParser sigFileParser = new SigFileParser(sigTestFile);
      final List<ClassDescription> classDescriptions = sigFileParser.create();
      final List<SimpleClassDescriptor> collect =
          classDescriptions.stream()
              .map(x -> ModelTransformer.transform(x))
              .collect(Collectors.toList());

      dbDocument.setClassDescriptions(collect);

      // write to db
      LOGGER.info("Start Writing Results to Mongo´ {}", sigTestFile.getFileName().toString());

      mongoDBHandler.addToDB(dbDocument);

      LOGGER.info("Done with Analysis");

    } catch (IOException ex) {
      LOGGER.error("failed to create sigtest file", ex);
    } finally {
      if (tempDirectory != null) {
        try {
          FileUtils.deleteDirectory(tempDirectory.toFile());
        } catch (IOException e) {
          LOGGER.error("Failed to delete tmp dir", e);
        }
      }
    }
  }
}
