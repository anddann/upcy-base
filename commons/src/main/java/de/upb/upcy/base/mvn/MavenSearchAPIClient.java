package de.upb.upcy.base.mvn;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.hash.Hashing;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import org.slf4j.LoggerFactory;

// https://search.maven.org/classic/#api
public class MavenSearchAPIClient {

  private static final org.slf4j.Logger LOGGER =
      LoggerFactory.getLogger(MavenSearchAPIClient.class);

  public static final String MAVEN_CENTRAL = "https://repo1.maven.org/maven2/";

  public static final String MAVEN_SEARCH_API = "https://search.maven.org";
  private static final ObjectMapper objectMapper = new ObjectMapper();

  private static final String CACHE_FOLDER =
      System.getenv("CACHE_DIR") == null ? "./cache" : System.getenv("CACHE_DIR");

  private MavenSearchAPIClient() {}

  // FIXME: add pagination
  // http://search.maven.org/solrsearch/select?q=g:"org.apache.maven.indexer"+AND+a:"maven-indexer"&rows=20&core=gav
  public static JsonNode getListOfArtifacts(String groupid, String artifactId) throws IOException {

    String queryURL =
        MAVEN_SEARCH_API
            + "/solrsearch/select?q="
            + URLEncoder.encode(String.format("g:\"%s\"", groupid), "UTF-8")
            + "+AND+"
            + URLEncoder.encode(String.format("a:\"%s\"", artifactId), "UTF-8")
            + "&rows=150&core=gav";
    URL url = new URL(queryURL);

    // cache code
    Path cacheDir = Paths.get(MavenSearchAPIClient.CACHE_FOLDER);
    if (!Files.exists(cacheDir)) {
      Files.createDirectories(cacheDir);
    }
    String cachedFile =
        Hashing.sha256().hashString(url.toString(), StandardCharsets.UTF_8).toString();

    Path fileName = cacheDir.resolve(cachedFile);
    if (Files.exists(fileName) && Files.isRegularFile(fileName)) {

      // we have the file cached
      LOGGER.debug("Cache Hit");

    } else {
      LOGGER.debug("Cache Miss");
      HttpURLConnection con = (HttpURLConnection) url.openConnection();
      con.setRequestMethod("GET");
      BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
      String inputLine;
      StringBuffer content = new StringBuffer();
      while ((inputLine = in.readLine()) != null) {
        content.append(inputLine);
      }
      in.close();
      con.disconnect();

      try (BufferedWriter writer =
          Files.newBufferedWriter(fileName, StandardCharsets.UTF_8, StandardOpenOption.CREATE)) {
        writer.write(content.toString());
      } catch (IOException ex) {
        ex.printStackTrace();
      }
    }

    return objectMapper.readTree(fileName.toFile());
  }
}
