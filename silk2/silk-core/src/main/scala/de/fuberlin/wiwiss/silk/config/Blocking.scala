package de.fuberlin.wiwiss.silk.config

import xml.Node

case class Blocking(blocks : Int = 100)
{
  require(blocks > 0, "blocks > 0")

  def toXML : Node =
  {
    <Blocking blocks={blocks.toString} />
  }
}

object Blocking
{
  def fromXML(node : Node) =
  {
    new Blocking(
      (node \ "@blocks").headOption.map(_.text.toInt).getOrElse(100)
    )
  }
}
