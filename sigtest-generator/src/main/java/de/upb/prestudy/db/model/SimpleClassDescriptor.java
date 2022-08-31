package de.upb.prestudy.db.model;

import java.util.ArrayList;

public class SimpleClassDescriptor {
  public String getSignature() {
    return signature;
  }

  public void setSignature(String signature) {
    this.signature = signature;
  }

  public ArrayList<String> getDeclFields() {
    return declFields;
  }

  public void setDeclFields(ArrayList<String> declFields) {
    this.declFields = declFields;
  }

  public ArrayList<String> getDeclMethods() {
    return declMethods;
  }

  public void setDeclMethods(ArrayList<String> declMethods) {
    this.declMethods = declMethods;
  }

  public ArrayList<String> getDeclConstructors() {
    return declConstructors;
  }

  public void setDeclConstructors(ArrayList<String> declConstructors) {
    this.declConstructors = declConstructors;
  }

  public ArrayList<String> getNestedClasses() {
    return nestedClasses;
  }

  public void setNestedClasses(ArrayList<String> nestedClasses) {
    this.nestedClasses = nestedClasses;
  }

  private String signature;
  private ArrayList<String> declFields = new ArrayList<>();
  private ArrayList<String> declMethods = new ArrayList<>();
  private ArrayList<String> declConstructors = new ArrayList<>();
  private ArrayList<String> nestedClasses = new ArrayList<>();
}
