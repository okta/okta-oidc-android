

package com.google.devtools.build.android.desugar;

import java.nio.file.Path;
import javax.annotation.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_Desugar_InputOutputPair extends Desugar.InputOutputPair {

  private final Path input;

  private final Path output;

  AutoValue_Desugar_InputOutputPair(
      Path input,
      Path output) {
    if (input == null) {
      throw new NullPointerException("Null input");
    }
    this.input = input;
    if (output == null) {
      throw new NullPointerException("Null output");
    }
    this.output = output;
  }

  @Override
  Path getInput() {
    return input;
  }

  @Override
  Path getOutput() {
    return output;
  }

  @Override
  public String toString() {
    return "InputOutputPair{"
         + "input=" + input + ", "
         + "output=" + output
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof Desugar.InputOutputPair) {
      Desugar.InputOutputPair that = (Desugar.InputOutputPair) o;
      return (this.input.equals(that.getInput()))
          && (this.output.equals(that.getOutput()));
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= input.hashCode();
    h$ *= 1000003;
    h$ ^= output.hashCode();
    return h$;
  }

}
