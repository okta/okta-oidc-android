

package com.google.devtools.build.android.desugar;

import com.google.common.collect.ImmutableList;
import javax.annotation.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_ClassSignatureParser_ClassSignature extends ClassSignatureParser.ClassSignature {

  private final String typeParameters;

  private final ClassSignatureParser.SuperclassSignature superClassSignature;

  private final ImmutableList<String> interfaceTypeParameters;

  AutoValue_ClassSignatureParser_ClassSignature(
      String typeParameters,
      ClassSignatureParser.SuperclassSignature superClassSignature,
      ImmutableList<String> interfaceTypeParameters) {
    if (typeParameters == null) {
      throw new NullPointerException("Null typeParameters");
    }
    this.typeParameters = typeParameters;
    if (superClassSignature == null) {
      throw new NullPointerException("Null superClassSignature");
    }
    this.superClassSignature = superClassSignature;
    if (interfaceTypeParameters == null) {
      throw new NullPointerException("Null interfaceTypeParameters");
    }
    this.interfaceTypeParameters = interfaceTypeParameters;
  }

  @Override
  String typeParameters() {
    return typeParameters;
  }

  @Override
  ClassSignatureParser.SuperclassSignature superClassSignature() {
    return superClassSignature;
  }

  @Override
  ImmutableList<String> interfaceTypeParameters() {
    return interfaceTypeParameters;
  }

  @Override
  public String toString() {
    return "ClassSignature{"
         + "typeParameters=" + typeParameters + ", "
         + "superClassSignature=" + superClassSignature + ", "
         + "interfaceTypeParameters=" + interfaceTypeParameters
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof ClassSignatureParser.ClassSignature) {
      ClassSignatureParser.ClassSignature that = (ClassSignatureParser.ClassSignature) o;
      return (this.typeParameters.equals(that.typeParameters()))
          && (this.superClassSignature.equals(that.superClassSignature()))
          && (this.interfaceTypeParameters.equals(that.interfaceTypeParameters()));
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= typeParameters.hashCode();
    h$ *= 1000003;
    h$ ^= superClassSignature.hashCode();
    h$ *= 1000003;
    h$ ^= interfaceTypeParameters.hashCode();
    return h$;
  }

}
