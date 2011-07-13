package de.fuberlin.wiwiss.silk.config

import xml.Node

case class Blocking(blocks : Int = Blocking.DefaultBlocks)
{
  require(blocks > 0, "blocks > 0")

  def toXML : Node =
  {
    <Blocking blocks={blocks.toString} />
  }
}

object Blocking
{
  val DefaultBlocks = 101

  def fromXML(node : Node) : Option[Blocking] =
  {
    val enabled = (node \ "@enabled").headOption.map(_.text.toBoolean) match
    {
      case Some(e) => e
      case None => true
    }

    if(enabled)
    {
      Some(Blocking(
        (node \ "@blocks").headOption.map(_.text.toInt).getOrElse(DefaultBlocks)
      ))
    }
    else
    {
      None
    }
  }
}
