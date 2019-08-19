

package com.google.devtools.build.android.desugar;

import javax.annotation.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_BytecodeTypeInference_InferredType extends BytecodeTypeInference.InferredType {

  private final String descriptor;

  AutoValue_BytecodeTypeInference_InferredType(
      String descriptor) {
    if (descriptor == null) {
      throw new NullPointerException("Null descriptor");
    }
    this.descriptor = descriptor;
  }

  @Override
  String descriptor() {
    return descriptor;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof BytecodeTypeInference.InferredType) {
      BytecodeTypeInference.InferredType that = (BytecodeTypeInference.InferredType) o;
      return (this.descriptor.equals(that.descriptor()));
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= descriptor.hashCode();
    return h$;
  }

}
