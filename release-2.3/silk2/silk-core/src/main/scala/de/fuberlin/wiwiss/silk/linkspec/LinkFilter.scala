package de.fuberlin.wiwiss.silk.linkspec

import xml.Node

case class LinkFilter(threshold : Double = 0.0, limit : Option[Int] = None)
{
  def toXML : Node = limit match
  {
    case Some(limit) => <Filter threshold={threshold.toString} limit={limit.toString} />
    case None => <Filter threshold={threshold.toString} />
  }
}

object LinkFilter
{
  def fromXML(node : Node) : LinkFilter =
  {
    val limitStr = (node \ "@limit").text

    LinkFilter((node \ "@threshold").text.toDouble, if(limitStr.isEmpty) None else Some(limitStr.toInt))
  }
}
