

package com.google.devtools.build.android.desugar;

import javax.annotation.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_ClassSignatureParser_SuperclassSignature extends ClassSignatureParser.SuperclassSignature {

  private final String identifier;

  private final String typeParameters;

  AutoValue_ClassSignatureParser_SuperclassSignature(
      String identifier,
      String typeParameters) {
    if (identifier == null) {
      throw new NullPointerException("Null identifier");
    }
    this.identifier = identifier;
    if (typeParameters == null) {
      throw new NullPointerException("Null typeParameters");
    }
    this.typeParameters = typeParameters;
  }

  @Override
  String identifier() {
    return identifier;
  }

  @Override
  String typeParameters() {
    return typeParameters;
  }

  @Override
  public String toString() {
    return "SuperclassSignature{"
         + "identifier=" + identifier + ", "
         + "typeParameters=" + typeParameters
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof ClassSignatureParser.SuperclassSignature) {
      ClassSignatureParser.SuperclassSignature that = (ClassSignatureParser.SuperclassSignature) o;
      return (this.identifier.equals(that.identifier()))
          && (this.typeParameters.equals(that.typeParameters()));
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= identifier.hashCode();
    h$ *= 1000003;
    h$ ^= typeParameters.hashCode();
    return h$;
  }

}
