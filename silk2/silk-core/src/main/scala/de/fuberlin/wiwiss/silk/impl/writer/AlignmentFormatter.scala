package de.fuberlin.wiwiss.silk.impl.writer

import de.fuberlin.wiwiss.silk.output.Link
import de.fuberlin.wiwiss.silk.util.plugin.Plugin

/**
 * Writes the alignment format specified at http://alignapi.gforge.inria.fr/format.html.
 */
@Plugin(id = "alignment", label = "Alignment", description = "Writes the alignment format specified at http://alignapi.gforge.inria.fr/format.html.")
class AlignmentFormatter() extends XMLFormatter {
  override def header = {
    "<?xml version='1.0' encoding='utf-8' standalone='no'?>\n" +
      "<rdf:RDF xmlns='http://knowledgeweb.semanticweb.org/heterogeneity/alignment#'\n" +
      "    xmlns:rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#'\n" +
      "    xmlns:xsd='http://www.w3.org/2001/XMLSchema#'\n" +
      "    xmlns:align='http://knowledgeweb.semanticweb.org/heterogeneity/alignment#'>\n" +
      "<Alignment>\n"
  }

  override def footer = {
    "</Alignment>\n" +
      "</rdf:RDF>\n"
  }

  override def formatXML(link: Link, predicateUri: String) = {
    <map>
      <Cell>
          <entity1 rdf:resource={link.source}/>
          <entity2 rdf:resource={link.target}/>
        <relation>
          {if (predicateUri == "http://www.w3.org/2002/07/owl#sameAs") "=" else predicateUri}
        </relation>
        <measure rdf:datatype="http://www.w3.org/2001/XMLSchema#float">
          {link.confidence.toString}
        </measure>
      </Cell>
    </map>
  }
}