package de.upb.upcy.base.sigtest.worker.sootdiff.apicheck;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import soot.ArrayType;
import soot.FastHierarchy;
import soot.Modifier;
import soot.RefType;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Type;

public class APICheck {

  private final FastHierarchy prevClassHierarchy;
  private final FastHierarchy nextClassHierarchy;
  private final Collection<SootClass> newClassScene;
  private final HashMap<String, SootClass> newClassesNames = new HashMap<>();

  public APICheck(
      FastHierarchy prevClassHierarchy,
      FastHierarchy nextClassHierarchy,
      Collection<SootClass> newClassScene) {
    this.prevClassHierarchy = prevClassHierarchy;
    this.nextClassHierarchy = nextClassHierarchy;
    this.newClassScene = newClassScene;
    newClassScene.forEach(x -> newClassesNames.put(x.getName(), x));
  }

  public static boolean isAPI(SootClass sootClass) {

    return sootClass.isPublic() || sootClass.isProtected();
  }

  public static boolean isAPI(SootMethod sootMethod) {
    if (!isAPI(sootMethod.getDeclaringClass())) {
      return false;
    }
    return sootMethod.isPublic() || sootMethod.isProtected();
  }

  public static boolean isAPI(SootField sootField) {
    if (!isAPI(sootField.getDeclaringClass())) {
      return false;
    }
    return sootField.isPublic() || sootField.isProtected();
  }

  public Collection<APICheckResult> isAPICompatible(
      SootClass prevClass, SootField prevField, SootClass nextClass) {
    Collection<APICheckResult> results = new ArrayList<>();

    if (!isAPI(prevField)) {
      // non api field
      APICheckResult apiCheckResult =
          new APICheckResult(
              prevField.getSignature(),
              nextClass.getName(),
              APICompatibility.NON_API_CODE,
              APICheckResult.APICheckType.FIELD);
      apiCheckResult.setDetailMessage("Non API Field");
      return Collections.singletonList(apiCheckResult);
    }

    Queue<SootClass> classToSearchForField = new ArrayDeque<>();
    SootField matchingField = null;
    //  check if it is moved to a compatible supertype
    classToSearchForField.add(nextClass);

    while (matchingField == null && !classToSearchForField.isEmpty()) {
      try {
        SootClass checkedClass = classToSearchForField.remove();
        if (!isAPI(checkedClass)) {
          continue;
        }

        matchingField = checkedClass.getField(prevField.getSubSignature());

        classToSearchForField.addAll(checkedClass.getInterfaces());
        classToSearchForField.add(checkedClass.getSuperclass());

      } catch (RuntimeException ex) {
        // nothing as this is expected
      }
    }
    if (prevField != null && matchingField == null) {

      classToSearchForField.clear();
      classToSearchForField.add(nextClass);

      while (matchingField == null && !classToSearchForField.isEmpty()) {
        try {
          SootClass checkedClass = classToSearchForField.remove();
          if (!isAPI(checkedClass)) {
            continue;
          }

          matchingField = checkedClass.getFieldByName(prevField.getName());
          // if field is final, the type can be widend, subtype is allowed, e.g., change Object
          // field ---> A field
          if (matchingField.isFinal()) {
            if (!isCompatible(prevField.getType(), matchingField.getType(), false)) {
              // not compatible, thus search further
              matchingField = null;
            }

          } else {
            // if field is NOT final, only supertypes are allowed, since they can also store
            // children,  e.g., change A field ---> Object field
            if (!isCompatible(prevField.getType(), matchingField.getType(), true)) {
              // not compatible, thus search further
              matchingField = null;
            }
          }

          classToSearchForField.addAll(checkedClass.getInterfaces());
          classToSearchForField.add(checkedClass.getSuperclass());

        } catch (RuntimeException ex) {
          // nothing as this is expected
        }
      }

      // field has been deleted
      APICheckResult apiCheckResult =
          new APICheckResult(
              prevField.getSignature(),
              nextClass.getName(),
              APICompatibility.DELETE,
              APICheckResult.APICheckType.FIELD);
      apiCheckResult.setDetailMessage("API Field deleted/not found");
      results.add(apiCheckResult);
    } else {

      // check modifiers
      final APICompatibility apiCompatibility =
          checkModifiers(prevField.getModifiers(), matchingField.getModifiers());

      if (!apiCompatibility.isCompatible()) {

        APICheckResult apiCheckResult =
            new APICheckResult(
                prevField.getSignature(),
                matchingField.getSignature(),
                apiCompatibility,
                APICheckResult.APICheckType.FIELD);
        apiCheckResult.setDetailMessage("API Field Modifiers incompatible");
        results.add(apiCheckResult);
      } else {
        APICheckResult apiCheckResult =
            new APICheckResult(
                prevField.getSignature(),
                matchingField.getSignature(),
                apiCompatibility,
                APICheckResult.APICheckType.FIELD);
        apiCheckResult.setDetailMessage("API Field is compatible");
        results.add(apiCheckResult);
      }
    }

    return results;
  }

  public Collection<APICheckResult> isAPICompatible(
      SootClass prevClass, SootMethod prevMethod, SootClass nextClass) {
    Collection<APICheckResult> results = new ArrayList<>();
    if (prevMethod == null) {
      // addition of a method
      APICheckResult apiCheckResult =
          new APICheckResult(
              prevClass.getName(),
              nextClass.getName(),
              APICompatibility.ADD,
              APICheckResult.APICheckType.METHOD);
      apiCheckResult.setDetailMessage("Method Added, no Prev method found");
      return Collections.singletonList(apiCheckResult);
    }
    if (!isAPI(prevMethod)) {
      // non-api methods are always non-breaking
      APICheckResult apiCheckResult =
          new APICheckResult(
              prevMethod.getName(),
              nextClass.getName(),
              APICompatibility.NON_API_CODE,
              APICheckResult.APICheckType.METHOD);
      apiCheckResult.setDetailMessage("Method is not API");
      return Collections.singletonList(apiCheckResult);
    }

    // method has been deleted
    final SootMethod matchingMethod =
        searchForMatchingMethod(prevClass, prevMethod, nextClass, true);

    if (matchingMethod == null) {

      APICheckResult apiCheckResult =
          new APICheckResult(
              prevMethod.getSignature(),
              nextClass.getName(),
              APICompatibility.DELETE,
              APICheckResult.APICheckType.METHOD);
      apiCheckResult.setDetailMessage("API Method deleted/not found");
      results.add(apiCheckResult);
    } else {

      // check 1. modifiers
      final APICompatibility apiCompatibility =
          checkModifiers(prevMethod.getModifiers(), matchingMethod.getModifiers());
      if (!apiCompatibility.isCompatible()) {

        APICheckResult apiCheckResult =
            new APICheckResult(
                prevMethod.getSignature(),
                matchingMethod.getSignature(),
                apiCompatibility,
                APICheckResult.APICheckType.METHOD);
        apiCheckResult.setDetailMessage("API Method Modifiers incompatible");
        results.add(apiCheckResult);
      } else {
        APICheckResult apiCheckResult =
            new APICheckResult(
                prevMethod.getSignature(),
                matchingMethod.getSignature(),
                apiCompatibility,
                APICheckResult.APICheckType.METHOD);
        apiCheckResult.setDetailMessage("API Method is compatible");
        results.add(apiCheckResult);
      }
    }

    return results;
  }

  public SootMethod searchForMatchingMethod(
      SootClass baseClass, SootMethod prevMethod, SootClass nextClass, boolean onlySearchForAPI) {
    // search for matching api method
    Queue<SootClass> classToSearchForMethod = new ArrayDeque<>();
    SootMethod matchingMethod = null;
    //  check if it is moved to a compatible supertype

    classToSearchForMethod.add(nextClass);

    while (matchingMethod == null && !classToSearchForMethod.isEmpty()) {
      try {
        SootClass checkedClass = classToSearchForMethod.remove();
        if (onlySearchForAPI && !isAPI(checkedClass)) {
          continue;
        }

        matchingMethod = checkedClass.getMethod(prevMethod.getSubSignature());

        classToSearchForMethod.addAll(checkedClass.getInterfaces());
        classToSearchForMethod.add(checkedClass.getSuperclass());

      } catch (RuntimeException ex) {
        // nothing as this is expected
      }
    }
    if (prevMethod != null && matchingMethod == null) {

      // check for method with same name; different but compatible types
      //  check if it is moved to a compatible supertype
      classToSearchForMethod.clear();
      classToSearchForMethod.add(nextClass);

      while (matchingMethod == null && !classToSearchForMethod.isEmpty()) {
        try {
          SootClass checkedClass = classToSearchForMethod.remove();
          if (onlySearchForAPI && !isAPI(checkedClass)) {
            continue;
          }
          // check for method with same name
          matchingMethod = checkedClass.getMethodByName(prevMethod.getName());
          // check if the method is compatbible
          final APICompatibility apiCompatibility =
              checkIfParameterAreCompatible(prevMethod, matchingMethod);
          if (!apiCompatibility.isCompatible()) {
            // search further
            matchingMethod = null;
          }

          classToSearchForMethod.addAll(checkedClass.getInterfaces());
          classToSearchForMethod.add(checkedClass.getSuperclass());

        } catch (RuntimeException ex) {
          // nothing as this is expected
        }
      }
    }

    return matchingMethod;
  }

  private APICompatibility checkIfParameterAreCompatible(
      SootMethod prevMethod, SootMethod nextMethod) {

    // quick check before going into classhierachy

    if (!StringUtils.equals(
        prevMethod.getReturnType().toQuotedString(), nextMethod.getReturnType().toQuotedString())) {
      // check for type compability if the names do not match
      final Type prevRetType = prevMethod.getReturnType();
      final Type returnType = nextMethod.getReturnType();
      if (!isCompatible(prevRetType, returnType, false)) {
        return APICompatibility.SIG_BREAK;
      }
    }

    if (prevMethod.getParameterCount() != nextMethod.getParameterCount()) {
      return APICompatibility.SIG_BREAK;
    }

    // check the parameters
    // TODO - deal with reordering
    for (int i = 0; i < prevMethod.getParameterCount(); i++) {
      Type prevParameterType = prevMethod.getParameterType(i);
      Type nextParameterType = nextMethod.getParameterType(i);

      if (!StringUtils.equals(
          prevParameterType.toQuotedString(), nextParameterType.toQuotedString())) {
        // names do not match check for compatibility of typs
        if (!isCompatible(prevParameterType, nextParameterType, true)) {
          return APICompatibility.SIG_BREAK;
        }
      }
    }
    return APICompatibility.NON_BREAKING;
  }

  /**
   * Checks whether an object of type "actual" can be inserted where an object of type "expected" is
   * required.
   *
   * @param actual The actual type (the substitution candidate)
   * @param expected The requested type
   * @return True if the two types are compatible and "actual" can be used as a substitute for
   *     "expected", otherwise false
   */
  private boolean isCompatible(SootClass actual, SootClass expected, boolean isParameter) {
    return isCompatible(actual.getType(), expected.getType(), isParameter);
  }

  /**
   * Checks whether an object of type "actual" can be inserted where an object of type "expected" is
   * required.
   *
   * @param oldType The actual type (the substitution candidate)
   * @param newType The requested type
   * @return True if the two types are compatible and "actual" can be used as a substitute for
   *     "expected", otherwise false
   */
  private boolean isCompatible(Type oldType, Type newType, boolean isParameter) {

    // quick check
    if (StringUtils.equals(oldType.toString(), newType.toQuotedString())) {
      return true;
    }

    // migrate to classes in new scene to allow reasonable type matching comparison
    if (oldType instanceof RefType) {
      RefType refType = (RefType) oldType;
      final SootClass sootClass = newClassesNames.get(refType.getClassName());
      if (sootClass != null) {
        refType.setSootClass(sootClass);
      }
    }
    if (oldType instanceof ArrayType) {
      ArrayType arrayType = (ArrayType) oldType;
      final SootClass sootClass = newClassesNames.get(arrayType.baseType);
      if (sootClass != null) {
        oldType = ArrayType.v(sootClass.getType(), oldType.getNumber());
      }
    }

    if (isParameter) {
      // parameters can be widend to be compatible, e.g., a supertype can be used (and client code
      // will continue to work)
      return nextClassHierarchy.canStoreType(oldType, newType);
    } else {
      // return types can be reducded, e.g, a subtype can be used (and clients code will continue to
      // work)
      return nextClassHierarchy.canStoreType(newType, oldType);
    }
  }

  public Collection<APICheckResult> isAPICompatible(SootClass prevClass, SootClass nextClass) {
    List<APICheckResult> results = new ArrayList<>();

    if (!isAPI(prevClass)) {
      APICheckResult apiCheckResult =
          new APICheckResult(
              prevClass.getName(),
              "",
              APICompatibility.NON_API_CODE,
              APICheckResult.APICheckType.CLASS);
      apiCheckResult.setDetailMessage("Non API Class");
      return Collections.singletonList(apiCheckResult);
    }

    if (prevClass == null && nextClass != null) {
      APICheckResult apiCheckResult =
          new APICheckResult(
              prevClass.getName(),
              nextClass.getName(),
              APICompatibility.ADD,
              APICheckResult.APICheckType.CLASS);
      return Collections.singletonList(apiCheckResult);
    }
    if (prevClass != null && nextClass == null) {
      APICheckResult apiCheckResult =
          new APICheckResult(
              prevClass.getName(), "", APICompatibility.DELETE, APICheckResult.APICheckType.CLASS);
      apiCheckResult.setDetailMessage("Class has been deleted");
      return Collections.singletonList(apiCheckResult);
    }
    final APICompatibility apiCompatibility =
        checkModifiers(prevClass.getModifiers(), nextClass.getModifiers());
    if (!apiCompatibility.isCompatible()) {
      APICheckResult apiCheckResult =
          new APICheckResult(
              prevClass.getName(),
              nextClass.getName(),
              apiCompatibility,
              APICheckResult.APICheckType.CLASS);
      results.add(apiCheckResult);
    }

    // check if API supertype has been removed
    ArrayList<SootClass> prevSuperTypes = new ArrayList<>(prevClass.getInterfaces());
    prevSuperTypes.add(prevClass.getSuperclass());

    ArrayList<SootClass> nextClassSuperTypes = new ArrayList<>(nextClass.getInterfaces());
    nextClassSuperTypes.add(nextClass.getSuperclass());

    for (SootClass prevSuperType : prevSuperTypes) {
      if (prevSuperType.getName().equals("java.lang.Object")) {
        continue;
      }
      if (isAPI(prevSuperType)) {
        // check if contained in next class or the hierachy of the next
        Queue<SootClass> superTypesToSearch = new ArrayDeque<>(nextClassSuperTypes);
        SootClass foundSuperType = null;

        while (foundSuperType == null && !superTypesToSearch.isEmpty()) {
          try {
            SootClass checkedClass = superTypesToSearch.remove();
            if (StringUtils.equals(prevSuperType.getName(), checkedClass.getName())) {
              foundSuperType = checkedClass;
            }
          } catch (RuntimeException ex) {
            // nothing as this is expected
          }
        }
        if (foundSuperType == null) {
          // supertype has been deleted from the hierarchy
          APICheckResult apiCheckResult =
              new APICheckResult(
                  prevClass.getName(),
                  nextClass.getName(),
                  APICompatibility.SUPER_TYPE_REMOVED,
                  APICheckResult.APICheckType.CLASS);
          apiCheckResult.setDetailMessage("API Supertype removed");
          results.add(apiCheckResult);
        }
      }
    }

    // check all methods
    for (SootMethod prevMethod : prevClass.getMethods()) {
      // constructors are checked below
      if (prevMethod.isConstructor()) {
        continue;
      }

      final Collection<APICheckResult> apiCompatible =
          isAPICompatible(prevClass, prevMethod, nextClass);

      results.addAll(apiCompatible);
    }

    // only execute constructor checks for concrete classes
    if (prevClass.isConcrete()) {

      // check if constructor has been added (and no-one existed before) -- this breaks api as the
      // default constructor is no longer available
      final List<SootMethod> prevConstructors =
          prevClass.getMethods().stream()
              .filter(m -> m.isConstructor())
              .collect(Collectors.toList());

      // check new constructors are added
      final List<SootMethod> newConstructors =
          nextClass.getMethods().stream()
              .filter(m -> m.isConstructor())
              .collect(Collectors.toList());
      // check if it still contains a default constructor

      // check if the old ones contaiend a default constructor
      final long oldDefConst =
          prevConstructors.stream()
              .filter(m -> m.getParameterCount() == 0)
              .filter(x -> isAPI(x))
              .count();

      final long newDefConstructors =
          newConstructors.stream()
              .filter(m -> m.getParameterCount() == 0)
              .filter(x -> isAPI(x))
              .count();
      if (newDefConstructors - oldDefConst < 0) {
        // the default constructor has been deleted
        APICheckResult apiCheckResult =
            new APICheckResult(
                prevClass.getName(),
                nextClass.getName(),
                APICompatibility.DELETE,
                APICheckResult.APICheckType.CONSTRUCTOR);
        apiCheckResult.setDetailMessage("Constructor(s) have been deleted");
        results.add(apiCheckResult);
      }
      // for all others do normal api method check
      for (SootMethod constructor : prevConstructors) {
        // skip the check for the default constructors
        if (constructor.getParameterCount() == 0) {
          continue;
        }
        Collection<APICheckResult> apiCompatible =
            isAPICompatible(prevClass, constructor, nextClass);
        apiCompatible.stream()
            .forEach(x -> x.setCheckType(APICheckResult.APICheckType.CONSTRUCTOR));
        results.addAll(apiCompatible);
      }
    }
    // check all fields
    for (SootField prevField : prevClass.getFields()) {
      final Collection<APICheckResult> apiCompatible =
          isAPICompatible(prevClass, prevField, nextClass);
      results.addAll(apiCompatible);
    }
    return results;
  }

  public static APICompatibility checkModifiers(int prev, int next) {
    // check visibility
    if (Modifier.isPublic(prev) && !Modifier.isPublic(next)) {
      return APICompatibility.V_DEC;
    }
    if (Modifier.isProtected(prev) && Modifier.isPrivate(next)) {
      return APICompatibility.V_DEC;
    }
    // check abstract
    if (!Modifier.isAbstract(prev) && Modifier.isAbstract(next)) {
      return APICompatibility.SIG_BREAK;
    }
    // check final
    if (!Modifier.isFinal(prev) && Modifier.isFinal(next)) {
      return APICompatibility.SIG_BREAK;
    }
    return APICompatibility.NON_BREAKING;
  }
}
