package de.upb.upcy.base.compatibility;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.upb.upcy.base.commons.Gav;
import de.upb.upcy.base.sigtest.MainCompareSigTest;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class SigABICompareProcess {

  /**
   * Run in process to separate from logic and kill sigtest after a timeout
   *
   * @param args
   * @throws IOException
   */
  public static void main(String[] args) throws IOException {

    String fileName = args[0];
    String jsonString = args[1];

    ObjectMapper objectMapper = new ObjectMapper();

    final Gav orgGav = objectMapper.readValue(jsonString, Gav.class);

    MainCompareSigTest mainCompareSigTest = new MainCompareSigTest();

    final Map<Gav, List<Gav>> orgNewGavs =
        MainComputeIncompatibilities.readInFile(Paths.get(fileName));
    mainCompareSigTest.handle(
        orgGav.group,
        orgGav.artifact,
        sigTestDBDocs ->
            MainComputeIncompatibilities.pairsForComparison(sigTestDBDocs, orgNewGavs));
  }
}
