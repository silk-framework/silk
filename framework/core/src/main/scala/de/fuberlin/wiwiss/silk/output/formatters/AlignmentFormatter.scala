package de.fuberlin.wiwiss.silk.output.formatters

import de.fuberlin.wiwiss.silk.output.Link

/**
 * Writes the alignment format specified at http://alignapi.gforge.inria.fr/format.html.
 */
class AlignmentFormatter extends XMLFormatter
{
    override def header =
    {
        "<?xml version='1.0' encoding='utf-8' standalone='no'?>\n" +
        "<rdf:RDF xmlns='http://knowledgeweb.semanticweb.org/heterogeneity/alignment#'\n" +
        "    xmlns:rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#'\n" +
        "    xmlns:xsd='http://www.w3.org/2001/XMLSchema#'\n" +
        "    xmlns:align='http://knowledgeweb.semanticweb.org/heterogeneity/alignment#'>\n" +
        "<Alignment>\n"
    }

    override def footer =
    {
        "</Alignment>\n" +
        "</rdf:RDF>\n"
    }

    override def formatXML(link : Link) =
    {
      <map>
        <Cell>
          <entity1 rdf:resource={link.sourceUri}/>
          <entity2 rdf:resource={link.targetUri}/>
          <relation>{if(link.predicate == "http://www.w3.org/2002/07/owl#sameAs") "=" else link.predicate}</relation>
          <measure rdf:datatype="http://www.w3.org/2001/XMLSchema#float">{link.confidence.toString}</measure>
        </Cell>
      </map>
    }
}
