

package com.google.devtools.build.android.desugar;

import com.google.common.collect.ImmutableList;
import javax.annotation.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_CoreLibrarySupport_EmulatedMethod extends CoreLibrarySupport.EmulatedMethod {

  private final int access;

  private final Class<?> owner;

  private final String name;

  private final String descriptor;

  private final ImmutableList<String> exceptions;

  AutoValue_CoreLibrarySupport_EmulatedMethod(
      int access,
      Class<?> owner,
      String name,
      String descriptor,
      ImmutableList<String> exceptions) {
    this.access = access;
    if (owner == null) {
      throw new NullPointerException("Null owner");
    }
    this.owner = owner;
    if (name == null) {
      throw new NullPointerException("Null name");
    }
    this.name = name;
    if (descriptor == null) {
      throw new NullPointerException("Null descriptor");
    }
    this.descriptor = descriptor;
    if (exceptions == null) {
      throw new NullPointerException("Null exceptions");
    }
    this.exceptions = exceptions;
  }

  @Override
  int access() {
    return access;
  }

  @Override
  Class<?> owner() {
    return owner;
  }

  @Override
  String name() {
    return name;
  }

  @Override
  String descriptor() {
    return descriptor;
  }

  @Override
  ImmutableList<String> exceptions() {
    return exceptions;
  }

  @Override
  public String toString() {
    return "EmulatedMethod{"
         + "access=" + access + ", "
         + "owner=" + owner + ", "
         + "name=" + name + ", "
         + "descriptor=" + descriptor + ", "
         + "exceptions=" + exceptions
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof CoreLibrarySupport.EmulatedMethod) {
      CoreLibrarySupport.EmulatedMethod that = (CoreLibrarySupport.EmulatedMethod) o;
      return (this.access == that.access())
          && (this.owner.equals(that.owner()))
          && (this.name.equals(that.name()))
          && (this.descriptor.equals(that.descriptor()))
          && (this.exceptions.equals(that.exceptions()));
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= access;
    h$ *= 1000003;
    h$ ^= owner.hashCode();
    h$ *= 1000003;
    h$ ^= name.hashCode();
    h$ *= 1000003;
    h$ ^= descriptor.hashCode();
    h$ *= 1000003;
    h$ ^= exceptions.hashCode();
    return h$;
  }

}
