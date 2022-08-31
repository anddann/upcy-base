package de.upb.upcy.base.updatesteps;

import de.upb.upcy.base.updatesteps.buildnrunpipeline.MultiProjectRunner;
import de.upb.upcy.base.updatesteps.buildnrunpipeline.Runner;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainBuildPipeline {
  /** Download link of the projects is https://zenodo.org/record/4479015#.Yf6JwvXMJhF */
  private static final Logger LOGGER = LoggerFactory.getLogger(MainBuildPipeline.class);

  public static void main(String[] args) throws IOException, InterruptedException {

    // create the command line parser
    CommandLineParser parser = new DefaultParser();

    // create the Options
    Options options = new Options();
    options.addOption(
        Option.builder("mp")
            .longOpt("multi-projects")
            .desc("the folder containing the projects")
            .hasArg()
            .build());
    options.addOption(
        Option.builder("sp")
            .longOpt("singe-project")
            .desc("the folder containing the project")
            .hasArg()
            .build());
    options.addOption(
        Option.builder("o")
            .longOpt("output")
            .desc("folder to store csv files")
            .hasArg()
            .required(true)
            .build());

    try {
      // parse the command line arguments
      CommandLine line = parser.parse(options, args);
      Path csvOutputDir = null;
      if (line.hasOption("output")) {
        // print the value of block-size
        csvOutputDir = Paths.get(line.getOptionValue("output"));
        if (!Files.exists(csvOutputDir)) {
          Files.createDirectories(csvOutputDir);
        }
      }
      Objects.requireNonNull(csvOutputDir);

      // validate that block-size has been set
      if (line.hasOption("singe-project")) {
        // print the value of block-size
        final String optionValue = line.getOptionValue("singe-project");

        final Path projectPom = Paths.get(optionValue).resolve("pom.xml");
        handleProject(projectPom.getParent().getFileName().toString(), projectPom, csvOutputDir);
      } else if (line.hasOption("multi-projects")) {
        // print the value of block-size
        final String optionValue = line.getOptionValue("multi-projects");
        handleMultiProject(optionValue, csvOutputDir);
      }
    } catch (ParseException exp) {
      LOGGER.error("Unexpected exception:" + exp.getMessage());
      // automatically generate the help statement
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("help", options);
    }
  }

  private static void handleMultiProject(String projectsRootFolder, Path outputDir)
      throws IOException, InterruptedException {

    MultiProjectRunner multiProjectRunner = new MultiProjectRunner(projectsRootFolder, outputDir);
    multiProjectRunner.run();
  }

  private static void handleProject(String projectName, Path projectPomFile, Path outputDir) {
    Runner projectRun = new Runner(projectName, projectPomFile, outputDir);
    projectRun.run();
  }
}
