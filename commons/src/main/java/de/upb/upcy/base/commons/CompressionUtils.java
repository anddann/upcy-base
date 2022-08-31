package de.upb.upcy.base.commons;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterOutputStream;

public class CompressionUtils {

  public static String compressAndReturnB64(String text) throws IOException {
    return new String(Base64.getEncoder().encode(compress(text)));
  }

  public static String decompressB64(String b64Compressed) throws IOException {
    byte[] decompressedBArray = decompress(Base64.getDecoder().decode(b64Compressed));
    return new String(decompressedBArray, StandardCharsets.UTF_8);
  }

  public static byte[] compress(String text) throws IOException {
    return compress(text.getBytes());
  }

  public static byte[] compress(byte[] bArray) throws IOException {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    try (DeflaterOutputStream dos = new DeflaterOutputStream(os)) {
      dos.write(bArray);
    }
    return os.toByteArray();
  }

  public static byte[] decompress(byte[] compressedTxt) throws IOException {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    try (OutputStream ios = new InflaterOutputStream(os)) {
      ios.write(compressedTxt);
    }

    return os.toByteArray();
  }

  public static String compressAndReturnB64(Path file, Charset encoding) throws IOException {
    byte[] encoded = Files.readAllBytes(file);
    final String s = new String(encoded, encoding);
    return compressAndReturnB64(s);
  }

  public static void decompressB64ToFile(String b64Compressed, Path file) throws IOException {
    final String s1 = decompressB64(b64Compressed);
    Files.write(file, s1.getBytes());
  }
}
