package org.netbeans.apitest;

import com.sun.tdk.signaturetest.Setup;
import com.sun.tdk.signaturetest.SignatureTest;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

/** Hack to override the org.netbeans.apitest.SigTest class to get more info */

// using split package to make public
public abstract class MySigtestHandler {

  static String[] ACTIONS =
      new String[] {
        "generate", "check", "strictcheck", "versioncheck", "binarycheck",
      };

  public final int execute() throws IOException {
    if (getPackages().equals("-")) {
      logInfo("No public packages, skipping");
      return 0;
    }
    boolean generate = false;
    boolean addBootCP = false;
    boolean onlySameVersion = false;
    List<String> arg = new ArrayList<String>();
    arg.add("-FileName");
    arg.add(getFileName().getAbsolutePath());
    if (getAction().equals("generate")) {
      generate = true;
      addBootCP = true;
      arg.add("-static");
      arg.add("-ErrorAll");
      arg.add("-KeepFile");
    } else if (getAction().equals("check") || getAction().equals("binarycheck")) {
      // no special arg for check
      arg.add("-static");
      arg.add("-b");
      arg.add("-Mode");
      arg.add("bin");
      addBootCP = true;
      if (getAction().equals("binarycheck")) {
        arg.add("-extensibleinterfaces");
      }
    } else if (getAction().equals("strictcheck")) {
      addBootCP = true;
      arg.add("-static");
    } else if (getAction().equals("versioncheck")) {
      addBootCP = true;
      arg.add("-static");
      onlySameVersion = true;
    } else {
      throw new IOException(
          "Unknown action: " + getAction() + " available actions are " + Arrays.toString(ACTIONS));
    }
    if (getVersion() != null) {
      arg.add("-ApiVersion");
      arg.add(getVersion());
    }
    logInfo("Packages: " + getPackages());
    StringTokenizer packagesTokenizer = new StringTokenizer(getPackages(), ",:;");
    while (packagesTokenizer.hasMoreTokens()) {
      String p = packagesTokenizer.nextToken().trim();
      String prefix = "-PackageWithoutSubpackages "; // NOI18N
      // Strip the ending ".*"
      int idx = p.lastIndexOf(".*");
      if (idx > 0) {
        p = p.substring(0, idx);
      } else {
        idx = p.lastIndexOf(".**");
        if (idx > 0) {
          prefix = "-Package "; // NOI18N
          p = p.substring(0, idx);
        }
      }
      arg.add(prefix.trim());
      arg.add(p);
    }
    if (getClasspath() != null) {
      StringBuffer sb = new StringBuffer();
      String pref = "";
      for (String e : getClasspath()) {
        sb.append(pref);
        sb.append(e);
        pref = File.pathSeparator;
      }
      if (addBootCP) {
        Integer release = getRelease();
        arg.add("-BootCP");
        if (release != null) {
          arg.add("" + release);
        }
      }
      arg.add("-Classpath");
      arg.add(sb.toString());
    }
    int returnCode;
    String[] args = arg.toArray(new String[0]);
    StringWriter output = new StringWriter();
    PrintWriter w = new PrintWriter(output, true);
    if (generate) {
      Setup t = new Setup();
      t.run(args, w, null);
      returnCode = t.isPassed() ? 0 : 1;
    } else {
      SignatureTest t = new SignatureTest();
      t.run(args, w, null);
      returnCode = t.isPassed() ? 0 : 1;
      // FIXME - AD --- add the final output to the report
      output.append("\n" + t.toString());
      if (onlySameVersion && !t.isPassed()) {
        // check the printed out versions
        final String prefix = "Base version: ";
        int index = output.toString().indexOf(prefix);
        if (index < 0) {
          throw new IOException("Missing " + prefix + " in:\n" + output.toString());
        }
        int end = output.toString().indexOf('\n', index);
        String base = output.toString().substring(index + prefix.length(), end);
        logInfo("versioncheck.TestedVersion: " + getVersion());
        logInfo("versioncheck.BaseVersion: " + base);
        if (!getVersion().equals(base)) {
          logInfo("versioncheck. clearing the return status.");
          returnCode = 0;
        }
      }
    }
    String out;
    if (getMail() != null) {
      out = "\nemail: " + getMail() + "\n" + output;
    } else {
      out = output.toString();
    }
    if (returnCode == 0) {
      logInfo(out);
    } else {
      logError(out);
    }
    boolean fail;
    if (getReport() != null) {
      writeReport(getReport(), out, returnCode == 0);
      fail = Boolean.TRUE.equals(isFailOnError());
    } else {
      fail = !Boolean.FALSE.equals(isFailOnError());
    }
    return fail ? returnCode : 0;
  }

  // do not wrap output in xml file
  protected void writeReport(File reportFile, String msg, boolean success) throws IOException {
    assert reportFile != null;
    reportFile.getParentFile().mkdirs();

    OutputStream os = new FileOutputStream(reportFile);
    try {
      byte[] strToBytes = msg.getBytes();
      os.write(strToBytes);
    } finally {
      os.close();
    }
    logInfo(reportFile + ": written in " + getFileName());
  }

  protected abstract Integer getRelease();

  protected abstract String getPackages();

  protected abstract File getFileName();

  protected abstract String getAction();

  protected abstract String getVersion();

  protected abstract String[] getClasspath();

  protected abstract File getReport();

  protected abstract String getMail();

  protected abstract Boolean isFailOnError();

  protected abstract void logInfo(String message);

  protected abstract void logError(String message);
}
