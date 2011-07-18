package de.fuberlin.wiwiss.silk.linkspec

import xml.Node
import de.fuberlin.wiwiss.silk.util.Identifier

trait Operator
{
  val id : Identifier
}

object Operator
{
  def generateId = Identifier("unnamed")

  def readId(xml : Node) : Identifier = {
    (xml \ "@id").headOption.map(_.text).map(Identifier(_)).getOrElse(generateId)
  }
}