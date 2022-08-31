package de.upb.prestudy.worker.sootdiff;

import com.google.common.base.Stopwatch;
import de.upb.prestudy.db.model.generate.SigTestDBDoc;
import de.upb.prestudy.db.model.sootdiff.BasicAPICheckDoc;
import de.upb.prestudy.db.model.sootdiff.CallGraphCheckDoc;
import de.upb.prestudy.worker.Utils;
import de.upb.prestudy.worker.sootdiff.apicheck.APICheck;
import de.upb.prestudy.worker.sootdiff.apicheck.APICheckResult;
import de.upb.soot.diff.SootDiff;
import de.upb.soot.diff.SootMethodDiffBuilder;
import de.upb.upcy.base.commons.ArtifactInfo;
import de.upb.upcy.base.commons.ArtifactUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.DiffBuilder;
import org.apache.commons.lang3.builder.DiffResult;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.LoggerFactory;
import soot.FastHierarchy;
import soot.G;
import soot.MethodOrMethodContext;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.VoidType;
import soot.jimple.spark.SparkTransformer;
import soot.jimple.toolkits.callgraph.CHATransformer;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Targets;

public class Processor {
  private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Processor.class);
  private final HashMap<String, Boolean> visited;

  private final HashMap<String, Boolean> changedMethodBody;

  private final Path temp_location;

  private static final String DIFF_DIR =
      System.getenv("DIFF_DIR") == null ? "./diffs" : System.getenv("DIFF_DIR");

  public Processor(Path tmpFolder) {
    temp_location = tmpFolder;
    changedMethodBody = new HashMap<>();
    visited = new HashMap<>();
  }

  public Pair<BasicAPICheckDoc, CallGraphCheckDoc> performAnalysis(
      SigTestDBDoc baseVersion, SigTestDBDoc nextVersion) {
    Path jarLocationBase = null;
    Path jarLocationNext = null;

    Pair result = Pair.of(null, null);

    ArtifactInfo baseVersionArtifact = baseVersion.getArtifactInfo();

    ArtifactInfo nextVersionArtifact = nextVersion.getArtifactInfo();

    try {

      Stopwatch stopwatch = Stopwatch.createStarted();
      // 2. Download file
      // 2.1 try by URL
      try {
        jarLocationBase = ArtifactUtils.downloadFilePlainURL(baseVersionArtifact, temp_location);
        LOGGER.info(
            "[Stats] Downloading {} took {}",
            jarLocationBase.getFileName().toString(),
            stopwatch.elapsed());

        jarLocationNext = ArtifactUtils.downloadFilePlainURL(nextVersionArtifact, temp_location);
        LOGGER.info(
            "[Stats] Downloading {} took {}",
            jarLocationNext.getFileName().toString(),
            stopwatch.elapsed());

      } catch (IOException ex) {
        LOGGER.error("Plain Downloaded file failed with: {}", ex.getMessage());
      }
      stopwatch.stop();
      if (jarLocationBase == null || jarLocationNext == null) {
        LOGGER.error("Failed to download the JAR files");
        return null;
      }

      /** Load the base version */
      SootDiff sootDiff =
          new SootDiff(
              Collections.singletonList(jarLocationBase.toAbsolutePath().toString()), null, false);

      sootDiff.runSootDiff();

      ArrayList<SootClass> prevVersionClasses = new ArrayList<>();

      // get the classes from the scene
      for (SootClass cl : Scene.v().getApplicationClasses()) {
        final SootClass sootClass = Scene.v().forceResolve(cl.getName(), SootClass.BODIES);
        prevVersionClasses.add(sootClass);
      }

      // get the hierarchy
      final FastHierarchy baseClassHierarchy = Scene.v().getOrMakeFastHierarchy();

      // build the callgraph
      List<SootMethod> entryPoints = new ArrayList<>();
      for (SootClass cl : Scene.v().getApplicationClasses()) {
        final SootClass sootClass = Scene.v().forceResolve(cl.getName(), SootClass.BODIES);
        for (SootMethod sootMethod : sootClass.getMethods()) {
          if (sootMethod.isPhantom()) {
            continue;
          }
          if (sootMethod.isConstructor()) {
            continue;
          }
          if (!sootMethod.isConcrete()) {
            continue;
          }
          if (sootMethod.isNative()) {
            continue;
          }
          if (!sootMethod.hasActiveBody()) {
            continue;
          }
          final boolean api = APICheck.isAPI(sootMethod);
          if (api) {
            entryPoints.add(sootMethod);
          }
        }
      }

      G.v().resetSpark();
      Scene.v().setEntryPoints(entryPoints);
      setCHA();
      // setSparkPointsToAnalysis();
      CallGraph cg = Scene.v().getCallGraph();

      /** Load the next version */
      sootDiff =
          new SootDiff(
              Collections.singletonList(jarLocationNext.toAbsolutePath().toString()), null, false);
      sootDiff.runSootDiff();

      ArrayList<SootClass> nextVersionClasses = new ArrayList<>();

      for (SootClass cl : Scene.v().getApplicationClasses()) {
        final SootClass sootClass = Scene.v().forceResolve(cl.getName(), SootClass.BODIES);
        nextVersionClasses.add(sootClass);
      }

      // get the hierarchy
      final FastHierarchy nextClassHierarchy = Scene.v().getOrMakeFastHierarchy();

      // do a basic API Check
      APICheck apiCheck = new APICheck(baseClassHierarchy, nextClassHierarchy, nextVersionClasses);

      final Collection<APICheckResult> results =
          runBasicAPICheck(prevVersionClasses, nextVersionClasses, apiCheck);

      BasicAPICheckDoc basicAPICheckDoc =
          wirteBasicAPICheckDoc2DB(baseVersionArtifact, nextVersionArtifact, results);

      result = Pair.of(basicAPICheckDoc, null);

      // run soot diff
      final Collection<DiffResult> diffResults =
          runSootDiffCheck(prevVersionClasses, nextVersionClasses, apiCheck);

      // dump the diff results into a folder

      this.writeDiffResults(baseVersionArtifact, nextVersionArtifact, diffResults);

      // compute the changed method bodys
      for (DiffResult diffResult : diffResults) {
        if (diffResult.getNumberOfDiffs() != 0) {
          // the body changed
          final Object left = diffResult.getLeft();
          changedMethodBody.put(((SootMethod) left).getSignature(), true);
        }
      }

      List<CallGraphCheckDoc.MethodCGAPI> brokenAPIMethodsInCG = new ArrayList<>();
      // idenitfy changes in non-api methods in the callgraph
      for (Iterator<MethodOrMethodContext> methodOrMethodContextIterator = cg.sourceMethods();
          methodOrMethodContextIterator.hasNext(); ) {
        MethodOrMethodContext methodOrMethodContext = methodOrMethodContextIterator.next();
        final SootMethod method = methodOrMethodContext.method();

        // check only entrypoint methods
        if (!entryPoints.contains(method)) {
          continue;
        }

        //  ignore auto generated lambda expression classes of the form __1272
        if (Utils.isAutoGeneratedLambdaClass(method.getDeclaringClass().getName())) {
          continue;
        }

        final Pair<Boolean, String> methodAPIbroken = visit(cg, method);
        if (methodAPIbroken.getLeft()) {
          // a method in the callchain has changed
          CallGraphCheckDoc.MethodCGAPI methodCGAPI = new CallGraphCheckDoc.MethodCGAPI();
          methodCGAPI.setStartMethod(method.getSignature());
          methodCGAPI.setChangedBodyMethod(methodAPIbroken.getRight());
          brokenAPIMethodsInCG.add(methodCGAPI);
        }
      }

      CallGraphCheckDoc callGraphCheckDoc = new CallGraphCheckDoc();
      callGraphCheckDoc.setBaseArtifact(baseVersionArtifact);
      callGraphCheckDoc.setNextArtifact(nextVersionArtifact);
      callGraphCheckDoc.setBrokenMethodsSignature(brokenAPIMethodsInCG);
      callGraphCheckDoc.setNumberOfIncompatibleMethods(brokenAPIMethodsInCG.size());
      callGraphCheckDoc.setNumberOfEntryPointMethods(entryPoints.size());

      result = Pair.of(basicAPICheckDoc, callGraphCheckDoc);

      return result;

    } catch (StringIndexOutOfBoundsException e) {
      LOGGER.error("Failed to run SigTest with ", e);
    } finally {
      // 5. Delete jar contents
      try {
        if (jarLocationBase != null) {
          Files.delete(jarLocationBase);
        }
        if (jarLocationNext != null) {
          Files.delete(jarLocationNext);
        }

      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return result;
  }

  private BasicAPICheckDoc wirteBasicAPICheckDoc2DB(
      ArtifactInfo baseVersionArtifact,
      ArtifactInfo nextVersionArtifact,
      Collection<APICheckResult> results) {
    // write the basic checks into the database
    BasicAPICheckDoc basicAPICheckDoc = new BasicAPICheckDoc();
    basicAPICheckDoc.setBaseArtifact(baseVersionArtifact);
    basicAPICheckDoc.setNextArtifact(nextVersionArtifact);

    // only store incompatibles in DB to ease validation
    List<APICheckResult> inComp =
        results.stream()
            .filter(x -> !x.getApiCompatibility().isCompatible())
            .collect(Collectors.toList());

    basicAPICheckDoc.setApiCheckResultList(new ArrayList<>(inComp));

    final long inCompMethods =
        inComp.stream()
            .filter(
                x ->
                    x.getCheckType() == APICheckResult.APICheckType.METHOD
                        || x.getCheckType() == APICheckResult.APICheckType.CONSTRUCTOR)
            .count();
    final long inCompClasses =
        inComp.stream().filter(x -> x.getCheckType() == APICheckResult.APICheckType.CLASS).count();
    final long inCompFields =
        inComp.stream().filter(x -> x.getCheckType() == APICheckResult.APICheckType.FIELD).count();

    basicAPICheckDoc.setNumberOfIncompatibleMethods((int) inCompMethods);
    basicAPICheckDoc.setNumberOfIncompatibleClasses((int) inCompClasses);
    basicAPICheckDoc.setGetNumberOfIncompatibleFields((int) inCompFields);
    return basicAPICheckDoc;
  }

  /**
   * Returns true if the method or callchain is api breacking otherwise false is returned returns
   * also the signature of the changed method
   *
   * @param cg
   * @param method
   * @return
   */
  private Pair<Boolean, String> visit(CallGraph cg, SootMethod method) {
    String identifier = method.getSignature();
    visited.put(identifier, true);
    // dot.drawNode(identifier);
    if (changedMethodBody.containsKey(identifier)) {
      return Pair.of(true, identifier);
    }

    // iterate over unvisited children
    Iterator<MethodOrMethodContext> ctargets = new Targets(cg.edgesOutOf(method));
    if (ctargets != null) {
      while (ctargets.hasNext()) {
        SootMethod child = (SootMethod) ctargets.next();
        // dot.drawEdge(identifier, child.getSignature());
        LOGGER.debug(method + " may call " + child);
        if (!visited.containsKey(child.getSignature())) {
          return visit(cg, child);
        }
      }
    }
    return Pair.of(false, null);
  }

  public void writeDiffResults(
      ArtifactInfo baseVersion, ArtifactInfo nextVersion, Collection<DiffResult> diffResults) {

    Path diffDir = Paths.get(Processor.DIFF_DIR);
    String base =
        String.format(
            "%s_%s_%s_%s",
            baseVersion.getGroupId(),
            baseVersion.getArtifactId(),
            baseVersion.getVersion(),
            baseVersion.getClassifier());
    String next =
        String.format(
            "%s_%s_%s_%s",
            nextVersion.getGroupId(),
            nextVersion.getArtifactId(),
            nextVersion.getVersion(),
            nextVersion.getClassifier());

    String resultsFolder = base + "___" + next;

    Path resultFolderPath = diffDir.resolve(resultsFolder);
    if (!Files.exists(resultFolderPath)) {
      try {
        Files.createDirectory(resultFolderPath);
      } catch (IOException e) {
        LOGGER.error("Failed to create directory:" + resultFolderPath, e);
        return;
      }
    }

    for (DiffResult diffResult : diffResults) {

      // use as a file name, the method's name
      final Object left = diffResult.getLeft();
      String fileName =
          ((SootMethod) left)
                  .getSignature()
                  .replace("(", "_")
                  .replace(")", "_")
                  .replace("<", "")
                  .replace(">", "")
              + ".json";
      if (fileName.length() > 255) {
        // cut down long filenames
        fileName = fileName.substring(0, 254);
      }
      try {
        Files.write(
            resultFolderPath.resolve(fileName),
            diffResult.toString().getBytes(),
            StandardOpenOption.CREATE_NEW);
      } catch (IOException e) {
        LOGGER.error("Failed to write Diff to File:" + fileName, e);
      }
    }
  }

  public Collection<DiffResult> runSootDiffCheck(
      Collection<SootClass> prevVersionClasses,
      Collection<SootClass> nextVersionClasses,
      APICheck apiCheck) {

    // do the API based check...
    HashMap<String, SootClass> baseClassMap = new HashMap<>();
    HashMap<String, SootClass> nextClassMap = new HashMap<>();

    prevVersionClasses.stream().forEach(x -> baseClassMap.put(x.getName(), x));
    nextVersionClasses.stream().forEach(x -> nextClassMap.put(x.getName(), x));

    Collection<DiffResult> diffResults = new ArrayList<>();
    for (Map.Entry<String, SootClass> baseClassEntry : baseClassMap.entrySet()) {
      SootClass nextClass = nextClassMap.get(baseClassEntry.getKey());

      SootClass prevClass = baseClassEntry.getValue();

      if (prevClass == null && nextClass != null) {
        continue;
      }
      if (prevClass != null && nextClass == null) {
        // is already covered in the api base check
        continue;
      }

      // run sootdiff on all methods (including public, private, protected)
      for (SootMethod prevMethod : Objects.requireNonNull(prevClass).getMethods()) {
        SootMethod matchingMethod =
            apiCheck.searchForMatchingMethod(prevClass, prevMethod, nextClass, false);
        if (matchingMethod == null) {
          // if no method has been found, create dummy method to print out meaning full diff
          SootMethod dummyMethod =
              Scene.v().makeSootMethod(prevMethod.getName(), Collections.emptyList(), VoidType.v());
          matchingMethod = dummyMethod;
        }
        DiffBuilder builder =
            new SootMethodDiffBuilder(prevMethod, matchingMethod, ToStringStyle.JSON_STYLE);
        DiffResult diffResult = builder.build();

        if (diffResult.getNumberOfDiffs() == 0 || StringUtils.isBlank(diffResult.toString())) {
          continue;
        }
        diffResults.add(diffResult);
      }
    }
    return diffResults;
  }

  public Collection<APICheckResult> runBasicAPICheck(
      Collection<SootClass> prevVersionClasses,
      Collection<SootClass> nextVersionClasses,
      APICheck apiCheck) {
    // check if they have incompatible / API breaking changes in the signature

    // do the API based check...
    HashMap<String, SootClass> baseClassMap = new HashMap<>();
    HashMap<String, SootClass> nextClassMap = new HashMap<>();

    prevVersionClasses.stream().forEach(x -> baseClassMap.put(x.getName(), x));
    nextVersionClasses.stream().forEach(x -> nextClassMap.put(x.getName(), x));

    Collection<APICheckResult> apiCheckResultCollection = new ArrayList<>();
    for (Map.Entry<String, SootClass> baseClassEntry : baseClassMap.entrySet()) {
      SootClass nextClass = nextClassMap.get(baseClassEntry.getKey());

      //  ignore auto generated lambda expression classes of the form __1272
      if (Utils.isAutoGeneratedLambdaClass(baseClassEntry.getValue().getName())) {
        continue;
      }

      final Collection<APICheckResult> apiCompatible =
          apiCheck.isAPICompatible(baseClassEntry.getValue(), nextClass);
      apiCheckResultCollection.addAll(apiCompatible);
    }
    return apiCheckResultCollection;
  }

  private void setSparkPointsToAnalysis() {
    LOGGER.info("[spark] Starting analysis ... \n");

    HashMap<String, String> opt = new HashMap();
    opt.put("enabled", "true");
    opt.put("verbose", "" + true);
    opt.put("ignore-types", "false");
    opt.put("force-gc", "false");
    opt.put("pre-jimplify", "false");
    opt.put("vta", "false");
    opt.put("rta", "false");
    opt.put("field-based", "false");
    opt.put("types-for-sites", "false");
    opt.put("merge-stringbuffer", "true");
    opt.put("string-constants", "false");
    opt.put("simulate-natives", "true");
    opt.put("simple-edges-bidirectional", "false");
    opt.put("on-fly-cg", "true");
    opt.put("simplify-offline", "false");
    opt.put("simplify-sccs", "false");
    opt.put("ignore-types-for-sccs", "false");
    opt.put("propagator", "worklist");
    opt.put("set-impl", "double");
    opt.put("double-set-old", "hybrid");
    opt.put("double-set-new", "hybrid");
    opt.put("dump-html", "false");
    opt.put("dump-pag", "false");
    opt.put("dump-solution", "false");
    opt.put("topo-sort", "false");
    opt.put("dump-types", "true");
    opt.put("class-method-var", "true");
    opt.put("dump-answer", "false");
    opt.put("add-tags", "false");
    opt.put("set-mass", "false");
    opt.put("apponly", "true");
    SparkTransformer.v().transform("", opt);
    LOGGER.info("[spark] Done!\n");
  }

  private void setCHA() {
    LOGGER.info("[soot-cha] Starting analysis ... \n");

    HashMap<String, String> opt = new HashMap();
    opt.put("enabled", "true");
    opt.put("verbose", "" + true);
    opt.put("ignore-types", "false");
    opt.put("force-gc", "false");
    opt.put("pre-jimplify", "false");
    opt.put("vta", "false");
    opt.put("rta", "false");
    opt.put("field-based", "false");
    opt.put("types-for-sites", "false");
    opt.put("merge-stringbuffer", "true");
    opt.put("string-constants", "false");
    opt.put("simulate-natives", "true");
    opt.put("simple-edges-bidirectional", "false");
    opt.put("on-fly-cg", "true");
    opt.put("simplify-offline", "false");
    opt.put("simplify-sccs", "false");
    opt.put("ignore-types-for-sccs", "false");
    opt.put("propagator", "worklist");
    opt.put("set-impl", "double");
    opt.put("double-set-old", "hybrid");
    opt.put("double-set-new", "hybrid");
    opt.put("dump-html", "false");
    opt.put("dump-pag", "false");
    opt.put("dump-solution", "false");
    opt.put("topo-sort", "false");
    opt.put("dump-types", "true");
    opt.put("class-method-var", "true");
    opt.put("dump-answer", "false");
    opt.put("add-tags", "false");
    opt.put("set-mass", "false");
    opt.put("apponly", "true");
    CHATransformer.v().transform("", opt);
    LOGGER.info("[soot-cha] Done!\n");
  }
}
