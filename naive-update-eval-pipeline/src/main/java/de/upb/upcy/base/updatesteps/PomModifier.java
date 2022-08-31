package de.upb.upcy.base.updatesteps;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PomModifier {
  private static final Logger LOGGER = LoggerFactory.getLogger(PomModifier.class);

  private final Path pomFile;

  public PomModifier(String pomFile) {
    this(Paths.get(pomFile));
  }

  public PomModifier(Path pomFile) {
    this.pomFile = pomFile;
    if (!Files.exists(this.pomFile) || !Files.isWritable(this.pomFile)) {
      LOGGER.error("Error accessing the pom file: {}", pomFile);
      throw new IllegalArgumentException("Pom file " + pomFile + "is not modifiable");
    }
  }

  public void addOrUpdate(String group, String artifact, String newVersion)
      throws DocumentException {
    // check if artifact exists, otherwise add id
    SAXReader reader = new SAXReader();
    Document document = reader.read(pomFile.toFile());
    //  only select direct dependencies -- not dependency in dependency mgmt
    // the local-name is used to ignore the pom xml namespace stuff
    // thats why everyone hates xml :(
    List<Node> list =
        document.selectNodes(
            "/project/*[local-name()='dependencies']/*[local-name()='dependency']");
    boolean foundNode = false;
    for (Iterator<Node> iter = list.iterator(); iter.hasNext(); ) {
      Node depNode = iter.next();
      final Node groupNode = depNode.selectSingleNode("*[local-name()='groupId']");
      final Node artNode = depNode.selectSingleNode("*[local-name()='artifactId']");
      final Node verNode = depNode.selectSingleNode("*[local-name()='version']");
      if (StringUtils.equals(groupNode.getStringValue(), group)
          && StringUtils.equals(artNode.getStringValue(), artifact)) {
        // update the version
        if (verNode != null) {
          verNode.setText(newVersion);
        } else {
          // the property is managed or inherited
          // thus, create a version node
          final Element versElement = ((Element) depNode).addElement("version");
          versElement.setText(newVersion);
        }
        foundNode = true;
        break;
      }
    }
    if (!foundNode) {
      // add it as a direct dependency
      List<Node> nodes = document.selectNodes("/project/dependencies");
      for (Node node : nodes) {
        // get the required node and add your new node to specific node.
        if (node instanceof Element) {
          Element e = (Element) node;
          final Element dependency = e.addElement("dependency");
          final Element groudElement = dependency.addElement("groupId");
          groudElement.setText(group);
          final Element artifactElement = dependency.addElement("artifactId");
          artifactElement.setText(artifact);
          final Element versElement = dependency.addElement("version");
          versElement.setText(newVersion);
        }
      }
    }
    // write out the pom file
    XMLWriter writer = null;
    try {
      writer = new XMLWriter(new FileWriter(pomFile.toFile()));
      writer.write(document);
    } catch (IOException e) {
      LOGGER.error("Failed to write file: {}", this.pomFile.toFile());
    } finally {
      try {
        if (writer != null) {
          writer.close();
        }
      } catch (IOException e) {
        // do nothing
      }
    }
  }
}
