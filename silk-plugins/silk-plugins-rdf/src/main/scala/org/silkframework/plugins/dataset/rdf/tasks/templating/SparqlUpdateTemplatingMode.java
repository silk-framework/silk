package org.silkframework.plugins.dataset.rdf.tasks.templating;

import org.silkframework.runtime.plugin.EnumerationParameterType;

/**
 *
 */
public enum SparqlUpdateTemplatingMode implements EnumerationParameterType {
  simple("simple", "Simple"),
  velocity("velocity", "Velocity Engine");

  private String id;
  private String displayName;

  SparqlUpdateTemplatingMode(String id, String displayName) {
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