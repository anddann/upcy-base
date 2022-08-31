package de.upb.upcy.base.commons;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.hash.Hashing;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;

public class ArtifactUtils {

  private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ArtifactUtils.class);

  private static final int CONNECT_TIMEOUT = 5 * 60000;
  private static final int READ_TIMEOUT = 5 * 60000;

  private static final String CACHE_FOLDER =
      System.getenv("CACHE_DIR") == null ? "./cache" : System.getenv("CACHE_DIR");

  public static URL constructURL(ArtifactInfo info) throws MalformedURLException {
    // CAUTION: the url is not the right download url. The download url ist replaced
    ArrayList<String> res = Lists.newArrayList();

    String classifier = "";
    if (info.getClassifier() != null) {
      classifier = "-" + info.getClassifier();
    }
    String repoURL = info.getRepoURL();
    if (Strings.isNullOrEmpty(repoURL)) {
      throw new IllegalArgumentException("Repo URL is blank");
    }

    if (repoURL.endsWith("/")) {
      repoURL = repoURL.substring(0, repoURL.length() - 1);
    }
    res.add(repoURL);
    String gId = info.getGroupId().replace(".", "/");
    res.add(gId);
    String aId = info.getArtifactId();
    res.add(aId);
    String vId = info.getVersion();
    res.add(vId);
    String fileExt = "jar";
    // filex= info.getP();
    res.add(aId + "-" + vId + classifier + "." + fileExt);
    return new URL(Joiner.on("/").join(res));
  }

  public static Path downloadFilePlainURL(ArtifactInfo info, Path downloadFolder)
      throws IOException {
    URL downloadURL = ArtifactUtils.constructURL(info);

    String classifier = "";
    if (info.getClassifier() != null) {
      classifier = "-" + info.getClassifier();
    }

    final Path cachedFile = lookUpInCache(downloadURL);

    // create copy to avoid that files in the cache are deleted
    Path jarName = cachedFile.getFileName();
    final Path target = downloadFolder.resolve(jarName);
    final Path copy = Files.copy(cachedFile, target);

    return copy;
  }

  private static Path lookUpInCache(URL downloadURL) throws IOException {

    downloadURL.getPath();
    String downloadPath = downloadURL.getPath();
    String jarName = downloadPath.substring(downloadPath.lastIndexOf("/") + 1);

    Path cacheDir = Paths.get(ArtifactUtils.CACHE_FOLDER);
    String cacheFolderHash =
        Hashing.sha256().hashString(downloadPath, StandardCharsets.UTF_8).toString();

    Path fileName = cacheDir.resolve(cacheFolderHash).resolve(jarName);
    if (Files.exists(fileName) && Files.isRegularFile(fileName)) {
      LOGGER.debug("Cache Hit");
      return fileName;
    } else {
      FileUtils.copyURLToFile(downloadURL, fileName.toFile(), CONNECT_TIMEOUT, READ_TIMEOUT);
      LOGGER.debug("Cache Miss");
      if (!Files.exists(fileName)) {
        throw new IOException("Failed to download jar: " + jarName);
      }
      LOGGER.info("Downloaded file from plain url: {}", downloadURL);
      return fileName;
    }
  }
}
