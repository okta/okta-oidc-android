

package com.google.devtools.build.android.desugar;

import com.google.common.collect.ImmutableList;
import javax.annotation.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_BytecodeTypeInference_FrameInfo extends BytecodeTypeInference.FrameInfo {

  private final ImmutableList<BytecodeTypeInference.InferredType> locals;

  private final ImmutableList<BytecodeTypeInference.InferredType> stack;

  AutoValue_BytecodeTypeInference_FrameInfo(
      ImmutableList<BytecodeTypeInference.InferredType> locals,
      ImmutableList<BytecodeTypeInference.InferredType> stack) {
    if (locals == null) {
      throw new NullPointerException("Null locals");
    }
    this.locals = locals;
    if (stack == null) {
      throw new NullPointerException("Null stack");
    }
    this.stack = stack;
  }

  @Override
  ImmutableList<BytecodeTypeInference.InferredType> locals() {
    return locals;
  }

  @Override
  ImmutableList<BytecodeTypeInference.InferredType> stack() {
    return stack;
  }

  @Override
  public String toString() {
    return "FrameInfo{"
         + "locals=" + locals + ", "
         + "stack=" + stack
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof BytecodeTypeInference.FrameInfo) {
      BytecodeTypeInference.FrameInfo that = (BytecodeTypeInference.FrameInfo) o;
      return (this.locals.equals(that.locals()))
          && (this.stack.equals(that.stack()));
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= locals.hashCode();
    h$ *= 1000003;
    h$ ^= stack.hashCode();
    return h$;
  }

}
