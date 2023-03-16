package org.silkframework.runtime.plugin.annotations;

public enum DistanceMeasureRange {

  NORMALIZED("This distance measure is normalized, i.e., all distances are between 0 (exact match) and 1 (no similarity)."),
  UNBOUND("This distance measure is not normalized, i.e., all distances start at 0 (exact match) and increase the more different the values are."),
  BOOLEAN("This is a boolean distance measure, i.e., all distances are either 0 or 1.");

  private final String description;

  DistanceMeasureRange(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}