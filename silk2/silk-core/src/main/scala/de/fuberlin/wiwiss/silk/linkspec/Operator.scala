package de.fuberlin.wiwiss.silk.linkspec

import xml.Node
import de.fuberlin.wiwiss.silk.util.Identifier
import java.util.concurrent.atomic.AtomicInteger

trait Operator
{
  val id : Identifier
}

object Operator
{
  private val lastId = new AtomicInteger(0)

  def generateId = Identifier("unnamed" + lastId.incrementAndGet())

  def readId(xml : Node) : Identifier = {
    (xml \ "@id").headOption.map(_.text).map(Identifier(_)).getOrElse(generateId)
  }
}