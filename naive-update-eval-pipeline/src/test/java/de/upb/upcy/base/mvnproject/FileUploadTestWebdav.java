package de.upb.upcy.base.mvnproject;

import de.upb.upcy.base.updatesteps.dockerize.FileServerUpload;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Ignore;
import org.junit.Test;

public class FileUploadTestWebdav {

  @Test
  @Ignore
  public void uploadTest() throws IOException {
    FileServerUpload fileServerUpload =
        new FileServerUpload(
            "https://uni-paderborn.sciebo.de/remote.php/dav/files/adann%40uni-paderborn.de/project_results",
            "adann@uni-paderborn.de", "PDAIW-TSMYH-MUZWH-JCGLC");
    fileServerUpload.uploadFileWebDav(Paths.get("/Users/adann/22.yml").toFile());
  }

  @Test
  @Ignore
  public void downloadTest() throws IOException {
    FileServerUpload fileServerUpload =
        new FileServerUpload(
            "https://uni-paderborn.sciebo.de/remote.php/dav/files/adann%40uni-paderborn.de/project_results",
            "adann@uni-paderborn.de", "PDAIW-TSMYH-MUZWH-JCGLC");
    final Path target = Paths.get("done_projects.txt");
    fileServerUpload.getFileWebDav("done_projects.txt", target);

    try (BufferedReader br = Files.newBufferedReader(target)) {
      String line;
      while ((line = br.readLine()) != null) {
        System.out.println(line);
      }
    }
  }
}
