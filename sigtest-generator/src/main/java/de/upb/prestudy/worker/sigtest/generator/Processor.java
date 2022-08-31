package de.upb.prestudy.worker.sigtest.generator;

import com.google.common.base.Stopwatch;
import de.upb.upcy.base.commons.ArtifactInfo;
import de.upb.upcy.base.commons.ArtifactUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.netbeans.apitest.MySigtestHandler;
import org.slf4j.LoggerFactory;

public class Processor {
  private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Processor.class);

  private final Path temp_location;

  public Processor(Path tmpFolder) throws IOException {
    temp_location = tmpFolder;
  }

  public Path performAnalysis(ArtifactInfo artifactInfo) {
    Path jarLocation = null;
    Path sigFileLocation = null;
    try {

      LOGGER.info("Processing Artifact#{} at url {}", artifactInfo, artifactInfo.getRepoURL());

      Stopwatch stopwatch = Stopwatch.createStarted();
      // 2. Download file
      // 2.1 try by URL
      try {
        jarLocation = ArtifactUtils.downloadFilePlainURL(artifactInfo, temp_location);
        sigFileLocation =
            temp_location.resolve(
                artifactInfo.getGroupId()
                    + "_"
                    + artifactInfo.getArtifactId()
                    + "_"
                    + artifactInfo.getVersion()
                    + ".sigfile");
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

      List<String> packages = new ArrayList<>();

      MySigtestHandler sigtestHandler =
          new MySigtestHandler() {

            @Override
            protected String getPackages() {
              return "";
              //              try {
              //                if (packages.isEmpty()) {
              //                  JarFile jar = new JarFile(finalJarLocation.toFile());
              //                  jar.stream()
              //                      .map(ZipEntry::getName)
              //                      .filter(name -> name.endsWith(".class"))
              //                      .map(name -> name.substring(0,
              // name.lastIndexOf('/')).replace('/', '.'))
              //                      .distinct()
              //                      .forEach(x -> packages.add(x));
              //                }
              //                return String.join(",", packages);
              //              } catch (IOException e) {
              //                return "";
              //              }
            }

            @Override
            protected File getFileName() {
              LOGGER.info("SigFile written:" + finalSigFileLocation.toAbsolutePath().toString());
              return finalSigFileLocation.toFile();
            }

            @Override
            protected String getAction() {
              return "generate";
            }

            @Override
            protected String getVersion() {
              return artifactInfo.getVersion();
            }

            @Override
            protected String[] getClasspath() {
              String[] cp = new String[1];
              // FIXME: only works for macos x
              cp[0] = finalJarLocation.toAbsolutePath().toString();
              // cp[1] = System.getProperty("java.home") + "/lib/rt.jar";
              return cp;
            }

            @Override
            protected File getReport() {
              return null;
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

      sigtestHandler.execute();

      return sigFileLocation;

    } catch (IOException | StringIndexOutOfBoundsException e) {
      LOGGER.error("Failed to run SigTest with ", e);
    } finally {
      // 5. Delete temp folder contents
      try {
        if (jarLocation != null) {
          Files.delete(jarLocation);
        }

      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return sigFileLocation;
  }
}
