package de.upb.prestudy.sigfileutils;

import com.sun.tdk.signaturetest.model.ClassDescription;
import com.sun.tdk.signaturetest.sigfile.FileManager;
import com.sun.tdk.signaturetest.sigfile.Reader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.LoggerFactory;

public class SigFileParser {
  private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(SigFileParser.class);

  private final Path sigFilePath;

  public SigFileParser(Path filePath) {
    sigFilePath = filePath;
  }

  public List<ClassDescription> create() {

    FileManager fm = new FileManager();

    ArrayList<ClassDescription> classDescriptions = new ArrayList<>();

    try {
      Reader in = fm.getReader(sigFilePath.toUri().toURL());

      if (in == null) {
        return null;
      }
      final boolean b = in.readSignatureFile(sigFilePath.toUri().toURL());

      String apiVersion = in.getApiVersion();

      ClassDescription cl;
      while ((cl = in.readNextClass()) != null) {

        String name = cl.getQualifiedName();
        classDescriptions.add(cl);
      }
    } catch (MalformedURLException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

    return classDescriptions;
  }
}
