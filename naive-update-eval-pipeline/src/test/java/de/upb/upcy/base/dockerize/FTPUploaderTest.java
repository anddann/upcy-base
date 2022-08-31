package de.upb.upcy.base.dockerize;

import de.upb.upcy.base.updatesteps.dockerize.FTPUploader;
import java.nio.file.Paths;
import org.junit.Ignore;
import org.junit.Test;

public class FTPUploaderTest {

  @Test
  @Ignore
  public void testUpload() throws Exception {
    final FTPUploader ftpUploader = new FTPUploader("localhost", 8080, "test", "superpw123");
    ftpUploader.uploadFile(Paths.get("/Users/adann/test3.zip").toFile(), "test3.zip", "/");
  }
}
