package de.upb.upcy.base.mvnproject;

import de.upb.upcy.base.updatesteps.dockerize.FileServerUpload;
import java.io.IOException;
import java.nio.file.Paths;
import org.junit.Ignore;
import org.junit.Test;

public class FileUploadTest {

  @Test
  @Ignore
  public void uploadTest() throws IOException {
    FileServerUpload fileServerUpload =
        new FileServerUpload("http://localhost:8000", "test", "test");
    fileServerUpload.uploadFile(Paths.get("/Users/adann/rabbitmq-mock.csv"));
  }

  @Test
  @Ignore
  public void uploadTestZip() throws IOException {
    FileServerUpload fileServerUpload =
        new FileServerUpload("http://localhost:8000", "test", "test");
    fileServerUpload.uploadFile(Paths.get("/Users/adann/test3.zip"));
  }

  public void uploadTest2() throws IOException {
    FileServerUpload fileServerUpload =
        new FileServerUpload("http://localhost:3000", "test", "test");
    fileServerUpload.uploadFile(Paths.get("/Users/adann/test3.zip"));
  }
}
