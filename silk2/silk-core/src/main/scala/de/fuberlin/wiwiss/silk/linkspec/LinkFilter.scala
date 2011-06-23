package de.fuberlin.wiwiss.silk.linkspec

import xml.Node
import java.util.logging.Logger

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
  private val logger = Logger.getLogger(LinkFilter.getClass.getName)

  /**
   * Creates a Link Filter from XML.
   */
  def fromXML(node : Node) : LinkFilter =
  {
    val limitStr = (node \ "@limit").text
    val threshold = (node \ "@threshold").headOption.map(_.text.toDouble)

    if(threshold.isDefined)
    {
      logger.warning("The use of a global threshold is deprecated. Please use per-comparison thresholds.")
    }

    LinkFilter(threshold.getOrElse(1.0), if(limitStr.isEmpty) None else Some(limitStr.toInt))
  }
}
