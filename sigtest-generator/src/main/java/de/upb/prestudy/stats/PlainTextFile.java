package de.upb.prestudy.stats;

import de.upb.prestudy.db.model.check.SigTestCheckDBDoc;
import de.upb.prestudy.db.model.generate.SigTestDBDoc;
import de.upb.prestudy.db.model.sootdiff.BasicAPICheckDoc;
import de.upb.prestudy.db.model.sootdiff.CallGraphCheckDoc;
import de.upb.prestudy.worker.sootdiff.apicheck.APICheckResult;
import de.upb.upcy.base.commons.CompressionUtils;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class PlainTextFile {

  private final Path resultFolderPath;
  private final Iterable<SigTestCheckDBDoc> sigCheck;
  private final Iterable<SigTestCheckDBDoc> sigCheckSource;

  private final Iterable<BasicAPICheckDoc> sootDiffBasicAPICheck;
  private final Iterable<CallGraphCheckDoc> sootDiffCGCheck;

  public PlainTextFile(
      Path resultFolderPath,
      Iterable<SigTestCheckDBDoc> sigCheck,
      Iterable<SigTestCheckDBDoc> sigCheckSource,
      Iterable<BasicAPICheckDoc> sootDiffBasicAPICheck,
      Iterable<CallGraphCheckDoc> sootDiffCGCheck) {
    this.resultFolderPath = resultFolderPath;
    this.sigCheck = sigCheck;
    this.sigCheckSource = sigCheckSource;
    this.sootDiffBasicAPICheck = sootDiffBasicAPICheck;
    this.sootDiffCGCheck = sootDiffCGCheck;
  }

  public void create(SigTestDBDoc baseVersion, SigTestDBDoc nextVersion) {
    try (BufferedWriter writer =
        Files.newBufferedWriter(resultFolderPath.resolve("plain.txt"), StandardCharsets.UTF_8)) {

      final String baseS = String.format("Base: %s", baseVersion.getArtifactInfo());
      writer.write(baseS);
      writer.newLine();

      final String nextS = String.format("Next: %s", nextVersion.getArtifactInfo());
      writer.write(nextS);
      writer.newLine();
      writer.newLine();

      final String sigCheckS = genSigTestString(sigCheck);

      writer.write(sigCheckS);
      writer.newLine();
      writer.newLine();

      final String sigCheckSourceS = genSigTestSourceString(sigCheckSource);

      writer.write(sigCheckSourceS);
      writer.newLine();
      writer.newLine();

      final String apiDiffS = genSootDiffAPIString(sootDiffBasicAPICheck);

      writer.write(apiDiffS);
      writer.newLine();
      writer.newLine();

      final String sootDiffCGS = genSootDiffCG(sootDiffCGCheck);

      writer.write(sootDiffCGS);
      writer.newLine();
      writer.newLine();

    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }

  private String genSootDiffCG(Iterable<CallGraphCheckDoc> sootDiffCGCheck) {
    StringBuilder sb = new StringBuilder();
    sb.append("## SootDiff - API Check\n");

    for (CallGraphCheckDoc sootDiffCheck : sootDiffCGCheck) {
      sb.append("Entrypoint Methods: " + sootDiffCheck.getNumberOfEntryPointMethods() + "\n");

      sb.append("Methods: " + sootDiffCheck.getNumberOfIncompatibleMethods() + "\n");

      for (CallGraphCheckDoc.MethodCGAPI methodCGAPI : sootDiffCheck.getBrokenMethodsSignature()) {
        sb.append(
            methodCGAPI.getStartMethod() + " ---> " + methodCGAPI.getChangedBodyMethod() + "\n");
      }
    }
    return sb.toString();
  }

  private String genSootDiffAPIString(Iterable<BasicAPICheckDoc> sootDiffBasicAPICheck) {
    StringBuilder sb = new StringBuilder();

    sb.append("## SootDiff - Basic API Check\n");

    for (BasicAPICheckDoc basicAPICheckDoc : sootDiffBasicAPICheck) {
      sb.append(String.format("Classes: %s\n", basicAPICheckDoc.getNumberOfIncompatibleClasses()));

      sb.append(String.format("Methods: %s\n", basicAPICheckDoc.getNumberOfIncompatibleMethods()));

      sb.append(String.format("Fields: %s\n", basicAPICheckDoc.getGetNumberOfIncompatibleFields()));

      for (APICheckResult result : basicAPICheckDoc.getApiCheckResultList()) {
        sb.append(
            String.format(
                "Prev: %s - Next: %s  Msg: %s Code: %s \n",
                result.getPrevElement(),
                result.getNextElement(),
                result.getDetailMessage(),
                result.getApiCompatibility().toString()));
      }
    }
    return sb.toString();
  }

  private String genSigTestString(Iterable<SigTestCheckDBDoc> sigCheck) {

    StringBuilder sb = new StringBuilder();

    sb.append("## SigCheck\n");

    for (SigTestCheckDBDoc sigTestCheckDBDoc : sigCheck) {
      sb.append(String.format("Classes: %s\n", sigTestCheckDBDoc.getNumberOfIncompatibleClasses()));

      sb.append(
          String.format("Constructs: %s\n", sigTestCheckDBDoc.getNumberOfIncompatibleConstructs()));

      try {
        final String s = CompressionUtils.decompressB64(sigTestCheckDBDoc.getReportFileContent());
        sb.append("SigCheck Content\n");
        sb.append(s);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return sb.toString();
  }

  private String genSigTestSourceString(Iterable<SigTestCheckDBDoc> sigCheck) {

    StringBuilder sb = new StringBuilder();

    sb.append("## SigCheck - Source \n");

    for (SigTestCheckDBDoc sigTestCheckDBDoc : sigCheck) {
      sb.append(String.format("Classes: %s\n", sigTestCheckDBDoc.getNumberOfIncompatibleClasses()));

      sb.append(
          String.format("Constructs: %s\n", sigTestCheckDBDoc.getNumberOfIncompatibleConstructs()));

      try {
        final String s = CompressionUtils.decompressB64(sigTestCheckDBDoc.getReportFileContent());
        sb.append("SigCheck Content\n");
        sb.append(s);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return sb.toString();
  }
}
