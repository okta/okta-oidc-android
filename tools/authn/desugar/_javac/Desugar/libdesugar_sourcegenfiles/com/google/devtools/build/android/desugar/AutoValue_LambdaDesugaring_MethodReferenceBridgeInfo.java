

package com.google.devtools.build.android.desugar;

import java.lang.reflect.Executable;
import javax.annotation.Generated;
import javax.annotation.Nullable;
import org.objectweb.asm.Handle;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_LambdaDesugaring_MethodReferenceBridgeInfo extends LambdaDesugaring.MethodReferenceBridgeInfo {

  private final Handle methodReference;

  private final Executable referenced;

  private final Handle bridgeMethod;

  AutoValue_LambdaDesugaring_MethodReferenceBridgeInfo(
      Handle methodReference,
      @Nullable Executable referenced,
      Handle bridgeMethod) {
    if (methodReference == null) {
      throw new NullPointerException("Null methodReference");
    }
    this.methodReference = methodReference;
    this.referenced = referenced;
    if (bridgeMethod == null) {
      throw new NullPointerException("Null bridgeMethod");
    }
    this.bridgeMethod = bridgeMethod;
  }

  @Override
  public Handle methodReference() {
    return methodReference;
  }

  @Nullable
  @Override
  public Executable referenced() {
    return referenced;
  }

  @Override
  public Handle bridgeMethod() {
    return bridgeMethod;
  }

  @Override
  public String toString() {
    return "MethodReferenceBridgeInfo{"
         + "methodReference=" + methodReference + ", "
         + "referenced=" + referenced + ", "
         + "bridgeMethod=" + bridgeMethod
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof LambdaDesugaring.MethodReferenceBridgeInfo) {
      LambdaDesugaring.MethodReferenceBridgeInfo that = (LambdaDesugaring.MethodReferenceBridgeInfo) o;
      return (this.methodReference.equals(that.methodReference()))
          && ((this.referenced == null) ? (that.referenced() == null) : this.referenced.equals(that.referenced()))
          && (this.bridgeMethod.equals(that.bridgeMethod()));
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= methodReference.hashCode();
    h$ *= 1000003;
    h$ ^= (referenced == null) ? 0 : referenced.hashCode();
    h$ *= 1000003;
    h$ ^= bridgeMethod.hashCode();
    return h$;
  }

}
