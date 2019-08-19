

package com.google.devtools.build.android.desugar;

import javax.annotation.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
final class AutoValue_MethodInfo extends MethodInfo {

  private final String owner;

  private final String name;

  private final String desc;

  AutoValue_MethodInfo(
      String owner,
      String name,
      String desc) {
    if (owner == null) {
      throw new NullPointerException("Null owner");
    }
    this.owner = owner;
    if (name == null) {
      throw new NullPointerException("Null name");
    }
    this.name = name;
    if (desc == null) {
      throw new NullPointerException("Null desc");
    }
    this.desc = desc;
  }

  @Override
  public String owner() {
    return owner;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public String desc() {
    return desc;
  }

  @Override
  public String toString() {
    return "MethodInfo{"
         + "owner=" + owner + ", "
         + "name=" + name + ", "
         + "desc=" + desc
        + "}";
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof MethodInfo) {
      MethodInfo that = (MethodInfo) o;
      return (this.owner.equals(that.owner()))
          && (this.name.equals(that.name()))
          && (this.desc.equals(that.desc()));
    }
    return false;
  }

  @Override
  public int hashCode() {
    int h$ = 1;
    h$ *= 1000003;
    h$ ^= owner.hashCode();
    h$ *= 1000003;
    h$ ^= name.hashCode();
    h$ *= 1000003;
    h$ ^= desc.hashCode();
    return h$;
  }

}
