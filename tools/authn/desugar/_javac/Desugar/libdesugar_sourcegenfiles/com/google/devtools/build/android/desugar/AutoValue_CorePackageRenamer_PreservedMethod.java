

package com.google.devtools.build.android.desugar;

import com.google.common.collect.ImmutableList;
import javax.annotation.Generated;
import javax.annotation.Nullable;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_CorePackageRenamer_PreservedMethod extends CorePackageRenamer.PreservedMethod {

  private final int access;

  private final String name;

  private final String desc;

  private final String signature;

  private final ImmutableList<String> exceptions;

  AutoValue_CorePackageRenamer_PreservedMethod(
      int access,
      String name,
      String desc,
      @Nullable String signature,
      ImmutableList<String> exceptions) {
    this.access = access;
    if (name == null) {
      throw new NullPointerException("Null name");
    }
    this.name = name;
    if (desc == null) {
      throw new NullPointerException("Null desc");
    }
    this.desc = desc;
    this.signature = signature;
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
  String name() {
    return name;
  }

  @Override
  String desc() {
    return desc;
  }

  @Nullable
  @Override
  String signature() {
    return signature;
  }

  @Override
  ImmutableList<String> exceptions() {
    return exceptions;
  }

  @Override
  public String toString() {
    return "PreservedMethod{"
         + "access=" + access + ", "
         + "name=" + name + ", "
         + "desc=" + desc + ", "
         + "signature=" + signature + ", "
         + "exceptions=" + exceptions
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof CorePackageRenamer.PreservedMethod) {
      CorePackageRenamer.PreservedMethod that = (CorePackageRenamer.PreservedMethod) o;
      return (this.access == that.access())
          && (this.name.equals(that.name()))
          && (this.desc.equals(that.desc()))
          && ((this.signature == null) ? (that.signature() == null) : this.signature.equals(that.signature()))
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
    h$ ^= name.hashCode();
    h$ *= 1000003;
    h$ ^= desc.hashCode();
    h$ *= 1000003;
    h$ ^= (signature == null) ? 0 : signature.hashCode();
    h$ *= 1000003;
    h$ ^= exceptions.hashCode();
    return h$;
  }

}
