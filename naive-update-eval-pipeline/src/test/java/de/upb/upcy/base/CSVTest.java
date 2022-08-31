package de.upb.upcy.base;

import com.opencsv.CSVWriter;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import de.upb.upcy.base.updatesteps.buildnrunpipeline.Result;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Test;

public class CSVTest {

  @Test
  public void testWriteRead() throws IOException {
    List<Result> resultList = new ArrayList<>();
    {
      Result result = new Result();
      result.setOrgGav("de.upb.orggav:1.0");
      result.setBuildResult(Result.OUTCOME.FAIL);

      List<String> errirs = new ArrayList<>();
      errirs.add("adawd");
      errirs.add("daadw");
      result.setTestErrors(errirs);
      resultList.add(result);
    }
    {
      Result result = new Result();
      result.setOrgGav("de.upb.orggav:2.0");
      result.setBuildResult(Result.OUTCOME.FAIL);

      resultList.add(result);
    }

    try {
      CSVWriter writer = new CSVWriter(new FileWriter("out.csv"));
      StatefulBeanToCsv sbc =
          new StatefulBeanToCsvBuilder(writer).withSeparator(CSVWriter.DEFAULT_SEPARATOR).build();
      sbc.write(resultList);
      writer.close();
    } catch (CsvRequiredFieldEmptyException | CsvDataTypeMismatchException | IOException e) {
      e.printStackTrace();
    }

    // read back in
    {
      List<Result> results;
      try (Reader reader = Files.newBufferedReader(Paths.get("out.csv"))) {
        CsvToBean sbc =
            new CsvToBeanBuilder(reader)
                .withType(Result.class)
                .withSeparator(CSVWriter.DEFAULT_SEPARATOR)
                .build();
        results = sbc.parse();
      }

      final Set<String> gavs = results.stream().map(Result::getOrgGav).collect(Collectors.toSet());
      System.out.println(gavs);
    }
  }
}
