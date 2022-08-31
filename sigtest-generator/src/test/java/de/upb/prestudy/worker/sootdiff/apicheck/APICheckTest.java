package de.upb.prestudy.worker.sootdiff.apicheck;

import de.upb.upcy.base.sigtest.worker.sootdiff.apicheck.APICheck;
import de.upb.upcy.base.sigtest.worker.sootdiff.apicheck.APICheckResult;
import de.upb.upcy.base.sigtest.worker.sootdiff.apicheck.APICompatibility;
import java.util.Collection;
import java.util.Collections;
import junit.framework.TestCase;
import soot.IntType;
import soot.Modifier;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;

public class APICheckTest extends TestCase {

  public void testIsAPI() {
    SootClass sootClass = Scene.v().makeSootClass("de.upb.TestClass");
    sootClass.setModifiers(Modifier.PUBLIC);
    final boolean api = APICheck.isAPI(sootClass);
    assertTrue(api);
  }

  public void testIsAPI2() {
    SootClass sootClass = Scene.v().makeSootClass("de.upb.TestClass");
    sootClass.setModifiers(Modifier.PRIVATE);
    final boolean api = APICheck.isAPI(sootClass);
    assertFalse(api);
  }

  public void testTestIsAPI() {
    SootClass sootClass = Scene.v().makeSootClass("de.upb.TestClass");
    sootClass.setModifiers(Modifier.PUBLIC);
    SootField sootField = Scene.v().makeSootField("testField", IntType.v());
    sootClass.addField(sootField);
    APICheck.isAPI(sootField);
  }

  public void testTestIsAPI1() {
    SootClass sootClass = Scene.v().makeSootClass("de.upb.TestClass");
    sootClass.setModifiers(Modifier.PUBLIC);

    SootMethod sootMethod =
        Scene.v().makeSootMethod("testMethod", Collections.emptyList(), IntType.v());
    sootMethod.setModifiers(Modifier.PUBLIC);
    sootClass.addMethod(sootMethod);
    APICheck.isAPI(sootMethod);
  }

  public void testIsAPICompatible() {
    SootClass sootClass = Scene.v().makeSootClass("de.upb.TestClass");
    sootClass.setModifiers(Modifier.PUBLIC);

    SootMethod sootMethod =
        Scene.v().makeSootMethod("testMethod", Collections.emptyList(), IntType.v());
    sootMethod.setModifiers(Modifier.PUBLIC);
    sootClass.addMethod(sootMethod);

    SootClass sootClass2 = Scene.v().makeSootClass("de.upb.TestClass");
    sootClass2.setModifiers(Modifier.PUBLIC);

    SootMethod sootMethod2 =
        Scene.v().makeSootMethod("testMethod", Collections.emptyList(), IntType.v());
    sootMethod2.setModifiers(Modifier.PUBLIC);
    sootClass2.addMethod(sootMethod2);

    APICheck apiCheck = new APICheck(null, null, Collections.emptyList());

    final Collection<APICheckResult> apiCompatible =
        apiCheck.isAPICompatible(sootClass, sootMethod, sootClass2);
    System.out.println(apiCompatible);
    assertEquals(1, apiCompatible.size());
    assertEquals(
        APICompatibility.NON_BREAKING,
        ((APICheckResult) apiCompatible.toArray()[0]).getApiCompatibility());
  }

  public void testTestIsAPICompatible() {
    SootClass sootClass = Scene.v().makeSootClass("de.upb.TestClass");
    sootClass.setModifiers(Modifier.PUBLIC);

    SootMethod sootMethod =
        Scene.v().makeSootMethod("testMethod", Collections.emptyList(), IntType.v());
    sootMethod.setModifiers(Modifier.PUBLIC);
    sootClass.addMethod(sootMethod);

    SootClass sootClass2 = Scene.v().makeSootClass("de.upb.TestClass");
    sootClass2.setModifiers(Modifier.PUBLIC);

    SootMethod sootMethod2 =
        Scene.v().makeSootMethod("testMethod", Collections.singletonList(IntType.v()), IntType.v());
    sootMethod2.setModifiers(Modifier.PUBLIC);
    sootClass2.addMethod(sootMethod2);

    APICheck apiCheck = new APICheck(null, null, Collections.emptyList());

    final Collection<APICheckResult> apiCompatible =
        apiCheck.isAPICompatible(sootClass, sootMethod, sootClass2);
    System.out.println(apiCompatible);
    assertEquals(1, apiCompatible.size());
    assertEquals(
        APICompatibility.DELETE,
        ((APICheckResult) apiCompatible.toArray()[0]).getApiCompatibility());
  }

  public void testTestIsAPICompatible1() {
    SootClass sootClass = Scene.v().makeSootClass("de.upb.TestClass");
    sootClass.setModifiers(Modifier.PUBLIC);

    SootMethod sootMethod =
        Scene.v().makeSootMethod("testMethod", Collections.emptyList(), IntType.v());
    sootMethod.setModifiers(Modifier.PUBLIC);
    sootClass.addMethod(sootMethod);

    SootClass sootClass2 = Scene.v().makeSootClass("de.upb.TestClass");
    sootClass2.setModifiers(Modifier.PUBLIC);

    SootMethod sootMethod2 =
        Scene.v().makeSootMethod("testMethod", Collections.emptyList(), IntType.v());
    sootMethod2.setModifiers(Modifier.PRIVATE);
    sootClass2.addMethod(sootMethod2);

    APICheck apiCheck = new APICheck(null, null, Collections.emptyList());

    final Collection<APICheckResult> apiCompatible =
        apiCheck.isAPICompatible(sootClass, sootMethod, sootClass2);
    System.out.println(apiCompatible);
    assertEquals(1, apiCompatible.size());
    assertEquals(
        APICompatibility.V_DEC,
        ((APICheckResult) apiCompatible.toArray()[0]).getApiCompatibility());
  }

  public void testCheckModifiers() {}
}
