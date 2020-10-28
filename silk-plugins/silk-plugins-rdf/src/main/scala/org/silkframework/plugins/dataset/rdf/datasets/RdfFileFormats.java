package org.silkframework.plugins.dataset.rdf.datasets;

import org.apache.jena.riot.RDFLanguages;
import org.silkframework.runtime.plugin.EnumerationParameterType;

/**
  * Parameter Enum to represent support RDF file formats.
  */
public enum RdfFileFormats implements EnumerationParameterType {
  autodetect("", "auto detect"),
  turtle("Turtle", "Turtle"),
  nTriples("N-Triples", "N-Triples"),
  nQuads("N-Quads", "N-Quads"),
  rdfXml("RDF/XML", "RDF/XML");

  private final String id;
  private final String displayName;

  RdfFileFormats(String id, String displayName) {
    this.id = id;
    this.displayName = displayName;
  }

  public String id() {
    return id;
  }

  public String displayName() {
    return displayName;
  }

  public boolean matchesId(String str) {
    return RDFLanguages.nameToLang(str.trim()).getName().equals(id);
  }
}