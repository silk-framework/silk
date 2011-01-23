package de.fuberlin.wiwiss.silk.linkspec

import xml.Node

case class LinkFilter(threshold : Double = 0.0, limit : Option[Int] = None)

object LinkFilter
{
  def fromXML(node : Node) : LinkFilter =
  {
    val limitStr = (node \ "@limit").text

    LinkFilter((node \ "@threshold").text.toDouble, if(limitStr.isEmpty) None else Some(limitStr.toInt))
  }
}
