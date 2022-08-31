package de.upb.prestudy.worker.sootdiff;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import de.upb.upcy.base.sigtest.db.MongoDBHandler;
import de.upb.upcy.base.sigtest.db.model.generate.SigTestDBDoc;
import de.upb.upcy.base.sigtest.db.model.sootdiff.BasicAPICheckDoc;
import de.upb.upcy.base.sigtest.db.model.sootdiff.CallGraphCheckDoc;
import de.upb.upcy.base.sigtest.worker.sootdiff.Processor;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Ignore;

@Ignore
public class ProcessorTest {

  //   test for
  //  Base: com.google.code.gson_gson_2.3.1
  //  Next: com.google.code.gson_gson_2.4

  public void testPerformAnalysis() throws IOException {
    final Path wadad = Files.createTempDirectory("wadad");
    Processor processor = new Processor(wadad);

    MongoDBHandler mongoDBHandler = MongoDBHandler.getInstance("localhost", "user", "example");

    final SigTestDBDoc base = mongoDBHandler.findBy("com.google.code.gson", "gson", "2.3.1");

    final SigTestDBDoc next = mongoDBHandler.findBy("com.google.code.gson", "gson", "2.4");

    final Pair<BasicAPICheckDoc, CallGraphCheckDoc> basicAPICheckDocCallGraphCheckDocPair =
        processor.performAnalysis(base, next);
    assertNotNull(basicAPICheckDocCallGraphCheckDocPair);
    assertNotNull(basicAPICheckDocCallGraphCheckDocPair.getKey());
    assertEquals(2, basicAPICheckDocCallGraphCheckDocPair.getKey().getApiCheckResultList().size());
    assertEquals(
        2L,
        basicAPICheckDocCallGraphCheckDocPair.getKey().getApiCheckResultList().stream()
            .filter(x -> !x.getApiCompatibility().isCompatible())
            .count());
  }

  /*
   Test for Lambda Classes
  Base: com.google.guava_guava_23.6-jre
  Next: com.google.guava_guava_23.6.1-jre
    */
  public void testAnalsisLamdba() throws IOException {
    final Path wadad = Files.createTempDirectory("wadad");
    Processor processor = new Processor(wadad);

    MongoDBHandler mongoDBHandler = MongoDBHandler.getInstance("localhost", "user", "example");

    final SigTestDBDoc base = mongoDBHandler.findBy("com.google.guava", "guava", "23.6-jre");

    final SigTestDBDoc next = mongoDBHandler.findBy("com.google.guava", "guava", "23.6.1-jre");

    final Pair<BasicAPICheckDoc, CallGraphCheckDoc> basicAPICheckDocCallGraphCheckDocPair =
        processor.performAnalysis(base, next);
    assertNotNull(basicAPICheckDocCallGraphCheckDocPair);
    assertNotNull(basicAPICheckDocCallGraphCheckDocPair.getKey());
    assertEquals(0, basicAPICheckDocCallGraphCheckDocPair.getKey().getApiCheckResultList().size());
  }

  /*
   Test for SootDiff Comparision
  Base: com.google.code.gson_gson_2.6.1
  Next: com.google.code.gson_gson_2.6.2
    */
  public void testAnalsisSootDiffBody() throws IOException {
    final Path wadad = Files.createTempDirectory("wadad");
    Processor processor = new Processor(wadad);

    MongoDBHandler mongoDBHandler = MongoDBHandler.getInstance("localhost", "user", "example");

    final SigTestDBDoc base = mongoDBHandler.findBy("com.google.code.gson", "gson", "2.6.1");

    final SigTestDBDoc next = mongoDBHandler.findBy("com.google.code.gson", "gson", "2.6.2");

    final Pair<BasicAPICheckDoc, CallGraphCheckDoc> basicAPICheckDocCallGraphCheckDocPair =
        processor.performAnalysis(base, next);
    assertNotNull(basicAPICheckDocCallGraphCheckDocPair);
    assertNotNull(basicAPICheckDocCallGraphCheckDocPair.getRight());
    System.out.println(
        basicAPICheckDocCallGraphCheckDocPair.getRight().getNumberOfEntryPointMethods());
    System.out.println(
        basicAPICheckDocCallGraphCheckDocPair.getRight().getNumberOfIncompatibleMethods());
  }

  /*
   Test for SootDiff Comparision
  Base: com.squareup.okhttp3_okhttp_4.9.0
  Next: com.squareup.okhttp3_okhttp_4.9.1
    */
  public void testAnalsisSootDiffBody2() throws IOException {
    final Path wadad = Files.createTempDirectory("wadad");
    Processor processor = new Processor(wadad);

    MongoDBHandler mongoDBHandler = MongoDBHandler.getInstance("localhost", "user", "example");

    final SigTestDBDoc base = mongoDBHandler.findBy("com.squareup.okhttp3", "okhttp", "4.9.0");

    final SigTestDBDoc next = mongoDBHandler.findBy("com.squareup.okhttp3", "okhttp", "4.9.1");

    final Pair<BasicAPICheckDoc, CallGraphCheckDoc> basicAPICheckDocCallGraphCheckDocPair =
        processor.performAnalysis(base, next);
    assertNotNull(basicAPICheckDocCallGraphCheckDocPair);
    assertNotNull(basicAPICheckDocCallGraphCheckDocPair.getRight());
    System.out.println(
        basicAPICheckDocCallGraphCheckDocPair.getRight().getNumberOfEntryPointMethods());
    System.out.println(
        basicAPICheckDocCallGraphCheckDocPair.getRight().getNumberOfIncompatibleMethods());
  }

  /*
  Test for SootDiff Comparision
  Base: com.squareup.okhttp3_okhttp_3.11.0
  Next: com.squareup.okhttp3_okhttp_3.12.0
   */
  public void testAnalsisSootDiffReturnTypeChangedToSubType() throws IOException {
    final Path wadad = Files.createTempDirectory("wadad");
    Processor processor = new Processor(wadad);

    MongoDBHandler mongoDBHandler = MongoDBHandler.getInstance("localhost", "user", "example");

    final SigTestDBDoc base = mongoDBHandler.findBy("com.squareup.okhttp3", "okhttp", "3.11.0");

    final SigTestDBDoc next = mongoDBHandler.findBy("com.squareup.okhttp3", "okhttp", "3.12.0");

    final Pair<BasicAPICheckDoc, CallGraphCheckDoc> basicAPICheckDocCallGraphCheckDocPair =
        processor.performAnalysis(base, next);
    assertNotNull(basicAPICheckDocCallGraphCheckDocPair);
    assertNotNull(basicAPICheckDocCallGraphCheckDocPair.getKey());
  }
}
