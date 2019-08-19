

package com.google.devtools.build.android.desugar;

import javax.annotation.Generated;
import org.objectweb.asm.Handle;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_LambdaInfo extends LambdaInfo {

  private final String desiredInternalName;

  private final String factoryMethodDesc;

  private final boolean needFactory;

  private final Handle methodReference;

  private final Handle bridgeMethod;

  AutoValue_LambdaInfo(
      String desiredInternalName,
      String factoryMethodDesc,
      boolean needFactory,
      Handle methodReference,
      Handle bridgeMethod) {
    if (desiredInternalName == null) {
      throw new NullPointerException("Null desiredInternalName");
    }
    this.desiredInternalName = desiredInternalName;
    if (factoryMethodDesc == null) {
      throw new NullPointerException("Null factoryMethodDesc");
    }
    this.factoryMethodDesc = factoryMethodDesc;
    this.needFactory = needFactory;
    if (methodReference == null) {
      throw new NullPointerException("Null methodReference");
    }
    this.methodReference = methodReference;
    if (bridgeMethod == null) {
      throw new NullPointerException("Null bridgeMethod");
    }
    this.bridgeMethod = bridgeMethod;
  }

  @Override
  public String desiredInternalName() {
    return desiredInternalName;
  }

  @Override
  public String factoryMethodDesc() {
    return factoryMethodDesc;
  }

  @Override
  public boolean needFactory() {
    return needFactory;
  }

  @Override
  public Handle methodReference() {
    return methodReference;
  }

  @Override
  public Handle bridgeMethod() {
    return bridgeMethod;
  }

  @Override
  public String toString() {
    return "LambdaInfo{"
         + "desiredInternalName=" + desiredInternalName + ", "
         + "factoryMethodDesc=" + factoryMethodDesc + ", "
         + "needFactory=" + needFactory + ", "
         + "methodReference=" + methodReference + ", "
         + "bridgeMethod=" + bridgeMethod
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof LambdaInfo) {
      LambdaInfo that = (LambdaInfo) o;
      return (this.desiredInternalName.equals(that.desiredInternalName()))
          && (this.factoryMethodDesc.equals(that.factoryMethodDesc()))
          && (this.needFactory == that.needFactory())
          && (this.methodReference.equals(that.methodReference()))
          && (this.bridgeMethod.equals(that.bridgeMethod()));
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= desiredInternalName.hashCode();
    h$ *= 1000003;
    h$ ^= factoryMethodDesc.hashCode();
    h$ *= 1000003;
    h$ ^= needFactory ? 1231 : 1237;
    h$ *= 1000003;
    h$ ^= methodReference.hashCode();
    h$ *= 1000003;
    h$ ^= bridgeMethod.hashCode();
    return h$;
  }

}
