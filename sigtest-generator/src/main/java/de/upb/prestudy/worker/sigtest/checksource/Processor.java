package de.upb.prestudy.worker.sigtest.checksource;

import com.google.common.base.Stopwatch;
import de.upb.prestudy.db.model.generate.SigTestDBDoc;
import de.upb.upcy.base.commons.ArtifactInfo;
import de.upb.upcy.base.commons.ArtifactUtils;
import de.upb.upcy.base.commons.CompressionUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.netbeans.apitest.MySigtestHandler;
import org.slf4j.LoggerFactory;

public class Processor {
  private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Processor.class);

  private final Path temp_location;

  public Processor(Path tmpFolder) {
    temp_location = tmpFolder;
  }

  public Path performAnalysis(SigTestDBDoc baseVersion, SigTestDBDoc nextVersion) {
    Path jarLocation = null;
    Path sigFileLocation = null;
    Path reportFile = null;

    ArtifactInfo nextVersionArtifact = nextVersion.getArtifactInfo();

    sigFileLocation =
        temp_location.resolve(
            baseVersion.getArtifactInfo().getGroupId()
                + "_"
                + baseVersion.getArtifactInfo().getArtifactId()
                + "_"
                + baseVersion.getArtifactInfo().getVersion()
                + ".sigfile");

    reportFile =
        temp_location.resolve(
            "compare_"
                + baseVersion.getArtifactInfo().getGroupId()
                + "_"
                + baseVersion.getArtifactInfo().getArtifactId()
                + "_"
                + baseVersion.getArtifactInfo().getVersion()
                + "__with__"
                + nextVersionArtifact.getGroupId()
                + "_"
                + nextVersionArtifact.getArtifactId()
                + "_"
                + nextVersionArtifact.getVersion()
                + ".xml");

    try {
      // unzip the content into a sigfile
      CompressionUtils.decompressB64ToFile(baseVersion.getSigTestFileContent(), sigFileLocation);
      LOGGER.info("SigFile  unpacked to:" + sigFileLocation.toAbsolutePath().toString());

      LOGGER.info(
          "Processing Artifact#{} at url {}",
          nextVersionArtifact,
          nextVersionArtifact.getRepoURL());

      Stopwatch stopwatch = Stopwatch.createStarted();
      // 2. Download file
      // 2.1 try by URL
      try {
        jarLocation = ArtifactUtils.downloadFilePlainURL(nextVersionArtifact, temp_location);

      } catch (IOException ex) {
        LOGGER.error("Plain Downloaded file failed with: {}", ex.getMessage());
      }
      stopwatch.stop();
      if (jarLocation == null) {
        return null;
      }
      LOGGER.info(
          "[Stats] Downloading {} took {}",
          jarLocation.getFileName().toString(),
          stopwatch.elapsed());

      // create a sigtest file

      Path finalJarLocation = jarLocation;
      Path finalSigFileLocation = sigFileLocation;
      Path finalReportFile = reportFile;
      MySigtestHandler sigtestHandler =
          new MySigtestHandler() {

            @Override
            protected String getPackages() {
              return "";
            }

            @Override
            protected File getFileName() {
              return finalSigFileLocation.toFile();
            }

            @Override
            // do source code checking
            protected String getAction() {
              return "strictcheck";
            }

            @Override
            protected String getVersion() {
              return nextVersionArtifact.getVersion();
            }

            @Override
            protected String[] getClasspath() {
              String[] cp = new String[1];
              // FIXME: only works for macos x
              cp[0] = finalJarLocation.toAbsolutePath().toString();
              //    cp[1] = System.getProperty("java.home") + "/lib/rt.jar";
              return cp;
            }

            @Override
            protected File getReport() {
              return finalReportFile.toFile();
            }

            @Override
            protected String getMail() {
              return null;
            }

            @Override
            protected Boolean isFailOnError() {
              return null;
            }

            @Override
            protected void logInfo(String message) {
              System.out.println(message);
            }

            @Override
            protected void logError(String message) {
              System.err.println(message);
            }

            @Override
            protected Integer getRelease() {
              return 8;
            }
          };

      final int execute = sigtestHandler.execute();

      return reportFile;

    } catch (IOException | ArrayIndexOutOfBoundsException | StringIndexOutOfBoundsException e) {
      LOGGER.error("Failed to run SigTest with ", e);
    } finally {
      // 5. Delete jar contents
      try {
        if (jarLocation != null) {
          Files.delete(jarLocation);
        }

      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return reportFile;
  }
}
