package de.upb.upcy.base.sigtest.stats;

import de.upb.upcy.base.commons.CompressionUtils;
import de.upb.upcy.base.sigtest.db.model.check.SigTestCheckDBDoc;
import de.upb.upcy.base.sigtest.db.model.generate.SigTestDBDoc;
import de.upb.upcy.base.sigtest.db.model.sootdiff.BasicAPICheckDoc;
import de.upb.upcy.base.sigtest.db.model.sootdiff.CallGraphCheckDoc;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class CSVFile {

  private final Path resultFolderPath;
  private final Iterable<SigTestCheckDBDoc> sigCheck;
  private final Iterable<SigTestCheckDBDoc> sigCheckSource;

  private final Iterable<BasicAPICheckDoc> sootDiffBasicAPICheck;
  private final Iterable<CallGraphCheckDoc> sootDiffCGCheck;

  public CSVFile(
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
        Files.newBufferedWriter(resultFolderPath.resolve("report.csv"), StandardCharsets.UTF_8)) {

      String header =
          "baseGroupId,baseArtifactId,baseVersion,nextGroupId,nextArtifactId,nextVersion,SigCheckClasses,SigCheckConstructs,SigCheckSourceClasses,SigCheckSourceConstructs,APICheckClasses,APICheckMethods,APICheckFields,APICheckConstructs,SootDiffMethods";

      writer.write(header);
      writer.newLine();

      writer.write(baseVersion.getArtifactInfo().getGroupId());
      writer.write(",");
      writer.write(baseVersion.getArtifactInfo().getArtifactId());
      writer.write(",");
      writer.write(baseVersion.getArtifactInfo().getVersion());
      writer.write(",");

      writer.write(nextVersion.getArtifactInfo().getGroupId());
      writer.write(",");
      writer.write(nextVersion.getArtifactInfo().getArtifactId());
      writer.write(",");
      writer.write(nextVersion.getArtifactInfo().getVersion());
      writer.write(",");

      final String sigCheckS = genSigTestString(sigCheck);

      writer.write(sigCheckS);

      final String sigCheckSourceS = genSigTestSourceString(sigCheckSource);

      writer.write(sigCheckSourceS);

      final String apiDiffS = genSootDiffAPIString(sootDiffBasicAPICheck);

      writer.write(apiDiffS);

      final String sootDiffCGS = genSootDiffCG(sootDiffCGCheck);

      writer.write(sootDiffCGS);
      writer.newLine();

    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }

  private String genSigTestString(Iterable<SigTestCheckDBDoc> sigCheck) {
    StringBuilder sb = new StringBuilder();

    for (SigTestCheckDBDoc sigTestCheckDBDoc : sigCheck) {
      // check if the file is broken
      try {
        String s = CompressionUtils.decompressB64(sigTestCheckDBDoc.getReportFileContent());
        if (s.contains("STATUS:Error.")) {

          continue;
        }
      } catch (IOException e) {
        //
      }

      sb.append(String.format("%s", sigTestCheckDBDoc.getNumberOfIncompatibleClasses()));
      sb.append(",");
      sb.append(String.format("%s", sigTestCheckDBDoc.getNumberOfIncompatibleConstructs()));
      sb.append(",");
    }
    // create empty input to maintain  consistent csv files
    if (sb.length() == 0) {
      sb.append(",");
      sb.append(",");
    }
    return sb.toString();
  }

  private String genSigTestSourceString(Iterable<SigTestCheckDBDoc> sigCheckSource) {
    StringBuilder sb = new StringBuilder();

    for (SigTestCheckDBDoc sigTestCheckDBDoc : sigCheckSource) {
      sb.append(String.format("%s", sigTestCheckDBDoc.getNumberOfIncompatibleClasses()));
      sb.append(",");
      sb.append(String.format("%s", sigTestCheckDBDoc.getNumberOfIncompatibleConstructs()));
      sb.append(",");
    }
    // create empty input to maintain  consistent csv files
    if (sb.length() == 0) {
      sb.append(",");
      sb.append(",");
    }
    return sb.toString();
  }

  private String genSootDiffAPIString(Iterable<BasicAPICheckDoc> sootDiffBasicAPICheck) {
    StringBuilder sb = new StringBuilder();

    for (BasicAPICheckDoc basicAPICheckDoc : sootDiffBasicAPICheck) {
      sb.append(String.format("%s", basicAPICheckDoc.getNumberOfIncompatibleClasses()));
      sb.append(",");

      sb.append(String.format("%s", basicAPICheckDoc.getNumberOfIncompatibleMethods()));
      sb.append(",");
      sb.append(String.format("%s", basicAPICheckDoc.getGetNumberOfIncompatibleFields()));
      sb.append(",");
      final int sumOfConstructs =
          basicAPICheckDoc.getNumberOfIncompatibleMethods()
              + basicAPICheckDoc.getGetNumberOfIncompatibleFields();
      sb.append(String.format("%s", sumOfConstructs));
      sb.append(",");
    }

    // create empty input to maintain  consistent csv files
    if (sb.length() == 0) {
      sb.append(",");
      sb.append(",");
      sb.append(",");
      sb.append(",");
    }
    return sb.toString();
  }

  private String genSootDiffCG(Iterable<CallGraphCheckDoc> sootDiffCGCheck) {
    StringBuilder sb = new StringBuilder();

    for (CallGraphCheckDoc sootDiffCheck : sootDiffCGCheck) {
      sb.append(sootDiffCheck.getNumberOfIncompatibleMethods());
    }
    // create empty input to maintain  consistent csv files
    if (sb.length() == 0) {
      sb.append("");
    }
    return sb.toString();
  }
}
