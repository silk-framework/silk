package org.silkframework.rule.plugins.distance.equality;

import org.silkframework.runtime.plugin.types.EnumerationParameterType;

/**
 * Parameter Enum to represent comparison operators.
 */
public enum OrderEnum implements EnumerationParameterType {

  alphabetical("Alphabetical", "Alphabetical"),
  numerical("Numerical", "Numerical"),
  integer("Integer", "Integer"),
  autodetect("Autodetect", "Autodetect");

  private String id;
  private String displayName;

  OrderEnum(String id, String displayName) {
    this.id = id;
    this.displayName = displayName;
  }

  public String id() {
    return id;
  }

  public String displayName() {
    return displayName;
  }
}