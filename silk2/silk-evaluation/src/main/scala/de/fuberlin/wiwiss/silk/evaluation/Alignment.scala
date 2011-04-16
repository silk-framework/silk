package de.fuberlin.wiwiss.silk.evaluation

import de.fuberlin.wiwiss.silk.output.Link
import xml.Node

case class Alignment(positiveLinks : Set[Link] = Set.empty, negativeLinks : Set[Link] = Set.empty)
{
  def generateNegative =
  {
    val positiveLinksSeq = positiveLinks.toSeq
    val sourceInstances = positiveLinksSeq.map(_.sourceUri)
    val targetInstances = positiveLinksSeq.map(_.targetUri)

    val negativeLinks = for((s, t) <- sourceInstances zip (targetInstances.tail :+ targetInstances.head)) yield new Link(s, t, 1.0)

    copy(negativeLinks = negativeLinks.toSet)
  }

  def toXML : Node =
  {
    <rdf:RDF xmlns='http://knowledgeweb.semanticweb.org/heterogeneity/alignment#'
             xmlns:rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#'
             xmlns:xsd='http://www.w3.org/2001/XMLSchema#'
             xmlns:align='http://knowledgeweb.semanticweb.org/heterogeneity/alignment#'>
      <Alignment>
        { serializeLinks(positiveLinks, "=") }
        { serializeLinks(negativeLinks, "!=") }
      </Alignment>
    </rdf:RDF>
  }

  private def serializeLinks(links : Traversable[Link], relation : String) : Seq[Node] =
  {
    for(link <- links.toList) yield
    {
      <map>
        <Cell>
          <entity1 rdf:resource={link.sourceUri}/>
          <entity2 rdf:resource={link.targetUri}/>
          <relation>{relation}</relation>
          <measure rdf:datatype="http://www.w3.org/2001/XMLSchema#float">{link.confidence.toString}</measure>
        </Cell>
      </map>
    }
  }
}
