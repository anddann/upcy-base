package de.upb.prestudy.db.model;

import com.sun.tdk.signaturetest.model.ClassDescription;
import com.sun.tdk.signaturetest.model.ConstructorDescr;
import com.sun.tdk.signaturetest.model.FieldDescr;
import com.sun.tdk.signaturetest.model.InnerDescr;
import com.sun.tdk.signaturetest.model.MethodDescr;
import java.util.ArrayList;

public class ModelTransformer {

  public static SimpleClassDescriptor transform(ClassDescription orgClassDescription) {
    SimpleClassDescriptor ownClassDescr = new SimpleClassDescriptor();
    ownClassDescr.setSignature(orgClassDescription.toString());

    ArrayList<String> declConst = new ArrayList<>();
    for (ConstructorDescr cons : orgClassDescription.getDeclaredConstructors()) {
      declConst.add(cons.toString());
    }
    ownClassDescr.setDeclConstructors(declConst);

    ArrayList<String> declMethods = new ArrayList<>();
    for (MethodDescr cons : orgClassDescription.getDeclaredMethods()) {
      declMethods.add(cons.toString());
    }
    ownClassDescr.setDeclMethods(declMethods);

    ArrayList<String> declFields = new ArrayList<>();
    for (FieldDescr cons : orgClassDescription.getDeclaredFields()) {
      declFields.add(cons.toString());
    }
    ownClassDescr.setDeclFields(declFields);

    ArrayList<String> nestClass = new ArrayList<>();
    for (InnerDescr cons : orgClassDescription.getDeclaredClasses()) {
      nestClass.add(cons.toString());
    }
    ownClassDescr.setNestedClasses(nestClass);

    return ownClassDescr;
  }
}
