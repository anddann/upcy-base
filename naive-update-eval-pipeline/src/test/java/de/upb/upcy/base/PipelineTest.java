package de.upb.upcy.base;

import de.upb.upcy.base.build.Utils;
import de.upb.upcy.base.graphanalysis.MainGraphAnalysis;
import de.upb.upcy.base.updatesteps.buildnrunpipeline.Runner;
import de.upb.upcy.base.updatesteps.dockerize.Msg;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Ignore;
import org.junit.Test;

public class PipelineTest {

  @Test
  @Ignore
  public void test() throws IOException, GitAPIException {

    Msg msg = new Msg();
    msg.setCommit("5b9c28c6");
    msg.setProjectName("geosolutions-it");
    msg.setRepoUrl("https://github.com/geosolutions-it/jai-ext.git");
    final Path path = Utils.checkOutRepo(msg.getRepoUrl(), msg.getCommit());
    Path projectPom = path.resolve("pom.xml");
    Path csvOutPutDir = Files.createTempDirectory(msg.getProjectName());

    Runner projectRun = new Runner(msg.getProjectName(), projectPom, csvOutPutDir);
    projectRun.run();

    MainGraphAnalysis.main(new String[] {csvOutPutDir.toAbsolutePath().toString()});
  }

  @Test
  @Ignore
  public void testCucumber() throws IOException, GitAPIException {

    Msg msg = new Msg();
    msg.setCommit("2b0b9151d");
    msg.setProjectName("cucumber-jvm");
    msg.setRepoUrl("https://github.com/cucumber/cucumber-jvm");
    final Path path = Utils.checkOutRepo(msg.getRepoUrl(), msg.getCommit());
    Path projectPom = path.resolve("pom.xml");
    Path csvOutPutDir = Files.createTempDirectory(msg.getProjectName());

    Runner projectRun = new Runner(msg.getProjectName(), projectPom, csvOutPutDir);
    projectRun.run();

    System.out.println(csvOutPutDir);
    MainGraphAnalysis.main(new String[] {csvOutPutDir.toAbsolutePath().toString()});
  }

  @Test
  @Ignore
  public void testCucumberGraphAnalysis() throws IOException, GitAPIException {

    Path projectPom =
        Paths.get(
            "/var/folders/j7/lnjf5fxd7mx1dxx5wzj4rb7r0000gn/T/cucumber-jvm9177315103211342455/cucumber-jvm.csv");

    MainGraphAnalysis.main(new String[] {projectPom.getParent().toAbsolutePath().toString()});
  }

  @Test
  // valid4j_valid4j1581646101347963538
  @Ignore
  public void testValid() throws IOException, GitAPIException {

    Msg msg = new Msg();
    msg.setCommit("2f3dd5e");
    msg.setProjectName("valid4j_valid4j");
    msg.setRepoUrl("https://github.com/valid4j/valid4j");
    final Path path = Utils.checkOutRepo(msg.getRepoUrl(), msg.getCommit());
    Path projectPom = path.resolve("pom.xml");
    Path csvOutPutDir = Files.createTempDirectory(msg.getProjectName());

    Runner projectRun = new Runner(msg.getProjectName(), projectPom, csvOutPutDir);
    projectRun.run();

    System.out.println(csvOutPutDir);
    MainGraphAnalysis.main(new String[] {csvOutPutDir.toAbsolutePath().toString()});
    System.out.println("Output dir: " + csvOutPutDir.toAbsolutePath());
  }
}
