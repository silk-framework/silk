package org.silkframework.rule;

import org.silkframework.runtime.plugin.EnumerationParameterType;

/**
 * Possible execution backends for the Silk linking execution.
 */
public enum LinkingExecutionBackend implements EnumerationParameterType {

  nativeExecution("native", "Native execution"),

  rdb("rdb", "RDBMS based execution");

  private String id;

  private String displayName;

  LinkingExecutionBackend(String id, String displayName) {
    this.id = id;
    this.displayName = displayName;
  }

  public String id() { return id; }

  public String displayName() { return displayName; }

}
