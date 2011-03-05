package de.fuberlin.wiwiss.silk.linkspec

import xml.Node

/**
 * A Link Filter specifies the parameters of the filtering phase.
 *
 * @param threshold Defines the minimum similarity of two data items which is required to generate a link between them.
 * @param limit Defines the number of links originating from a single data item. Only the n highest-rated links per source data item will remain after the filtering.
 */
case class LinkFilter(threshold : Double = 0.0, limit : Option[Int] = None)
{
  /**
   * Serializes this Link Filter as XML.
   */
  def toXML : Node = limit match
  {
    case Some(limit) => <Filter threshold={threshold.toString} limit={limit.toString} />
    case None => <Filter threshold={threshold.toString} />
  }
}

object LinkFilter
{
  /**
   * Creates a Link Filter from XML.
   */
  def fromXML(node : Node) : LinkFilter =
  {
    val limitStr = (node \ "@limit").text

    LinkFilter((node \ "@threshold").text.toDouble, if(limitStr.isEmpty) None else Some(limitStr.toInt))
  }
}
