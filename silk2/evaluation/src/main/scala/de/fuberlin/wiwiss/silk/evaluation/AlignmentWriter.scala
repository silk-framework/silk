package de.fuberlin.wiwiss.silk.evaluation

import xml.Node
import de.fuberlin.wiwiss.silk.output.Link

object AlignmentWriter
{
  def serialize(alignment : Traversable[Link]) : Node =
  {
    <rdf:RDF xmlns='http://knowledgeweb.semanticweb.org/heterogeneity/alignment#'
             xmlns:rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#'
             xmlns:xsd='http://www.w3.org/2001/XMLSchema#'
             xmlns:align='http://knowledgeweb.semanticweb.org/heterogeneity/alignment#'>
      <Alignment>{
        for(link <- alignment) yield
        {
          <map>
            <Cell>
              <entity1 rdf:resource={link.sourceUri}/>
              <entity2 rdf:resource={link.targetUri}/>
              <relation>=</relation>
              <measure rdf:datatype="http://www.w3.org/2001/XMLSchema#float">{link.confidence.toString}</measure>
            </Cell>
          </map>
        }
      }</Alignment>
    </rdf:RDF>
  }
}