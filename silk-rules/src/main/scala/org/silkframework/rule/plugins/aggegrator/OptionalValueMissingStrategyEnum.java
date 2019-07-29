package org.silkframework.rule.plugins.aggegrator;

import org.silkframework.runtime.plugin.EnumerationParameterType;

/**
 * Strategy when the value of a linking operator is missing for the OptionalAggregator aggregator.
 */
public enum OptionalValueMissingStrategyEnum implements EnumerationParameterType {

  Ignore("ignore", "Ignore"),

  True("true", "Evaluate to True"),

  False("false", "Evaluate to False");

  private String id;

  private String displayName;

  OptionalValueMissingStrategyEnum(String id, String displayName) {
    this.id = id;
    this.displayName = displayName;
  }

  public String id() { return id; }

  public String displayName() { return displayName; }
}