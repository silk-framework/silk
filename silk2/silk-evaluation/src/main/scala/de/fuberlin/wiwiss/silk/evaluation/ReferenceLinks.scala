package de.fuberlin.wiwiss.silk.evaluation

import de.fuberlin.wiwiss.silk.output.Link
import xml.Node

/**
 * A set of positive and negative reference links.
 */
case class ReferenceLinks(positive: Set[Link] = Set.empty, negative: Set[Link] = Set.empty) {
  /**
   * Adds a new positive reference link and returns the updated reference links.
   * Removes the given link from the negative reference links if it is contained in it.
   */
  def withPositive(link: Link) = {
    ReferenceLinks(positive + link, negative - link)
  }

  /**
   * Adds a new negative reference link and returns the updated reference links.
   * Removes the given link from the positive reference links if it is contained in it.
   */
  def withNegative(link: Link) = {
    ReferenceLinks(positive - link, negative + link)
  }

  /**
   * Removes a link and returns the updated reference links.
   */
  def without(link: Link) = {
    ReferenceLinks(positive - link, negative - link)
  }

  /**
   * Generates negative reference links from the positive reference links.
   */
  def generateNegative = {
    val positiveLinksSeq = positive.toSeq
    val sourceEntities = positiveLinksSeq.map(_.source)
    val targetEntities = positiveLinksSeq.map(_.target)

    val negativeLinks = for ((s, t) <- sourceEntities zip (targetEntities.tail :+ targetEntities.head)) yield new Link(s, t, 1.0)

    copy(negative = negativeLinks.toSet)
  }

  /**
   * True, if there are no reference links at all.
   */
  def isEmpty = positive.isEmpty && negative.isEmpty

  /**
   * True, if there are positive as well as negative reference links.
   */
  def isComplete = !positive.isEmpty && !negative.isEmpty

  /**
   * Serializes reference links as XML using the alignment format specified at http://alignapi.gforge.inria.fr/format.html.
   */
  def toXML: Node = {
    <rdf:RDF xmlns='http://knowledgeweb.semanticweb.org/heterogeneity/alignment#'
             xmlns:rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#'
             xmlns:xsd='http://www.w3.org/2001/XMLSchema#'
             xmlns:align='http://knowledgeweb.semanticweb.org/heterogeneity/alignment#'>
      <Alignment>
        { serializeLinks(positive, "=") }
        { serializeLinks(negative, "!=") }
      </Alignment>
    </rdf:RDF>
  }

  private def serializeLinks(links: Traversable[Link], relation: String): Seq[Node] = {
    for (link <- links.toList) yield {
      <map>
        <Cell>
          <entity1 rdf:resource={link.source}/>
          <entity2 rdf:resource={link.target}/>
          <relation>{relation}</relation>
          <measure rdf:datatype="http://www.w3.org/2001/XMLSchema#float">{link.confidence.toString}</measure>
        </Cell>
      </map>
    }
  }
}

/**
 * Loads reference links.
 */
object ReferenceLinks {
  /**
   * Reads reference links specified in the alignment format specified at http://alignapi.gforge.inria.fr/format.html.
   */
  def fromXML(node: Node) = ReferenceLinksReader.readReferenceLinks(node)
}
