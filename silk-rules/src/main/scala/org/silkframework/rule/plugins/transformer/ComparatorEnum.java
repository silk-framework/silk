package org.silkframework.rule.plugins.transformer;

import org.silkframework.runtime.plugin.types.EnumerationParameterType;

/**
 * Parameter Enum to represent comparison operators.
 */
public enum ComparatorEnum implements EnumerationParameterType {
  less("<", "<"),
  lessEqual("<=", "<="),
  equal("=", "="),
  greaterEqual(">=", ">="),
  greater(">", ">");

  private String id;
  private String displayName;

  ComparatorEnum(String id, String displayName) {
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