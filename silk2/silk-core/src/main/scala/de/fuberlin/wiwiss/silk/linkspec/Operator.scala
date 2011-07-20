package de.fuberlin.wiwiss.silk.linkspec

import xml.Node
import de.fuberlin.wiwiss.silk.util.Identifier
import java.util.concurrent.atomic.AtomicInteger

/**
 * Base class of all operators in the link condition.
 */
trait Operator {
  val id : Identifier
}

/**
 * Operator companion object.
 */
object Operator {
  /** Counter used to generate unique identifiers. */
  private val lastId = new AtomicInteger(0)

  /**
   * Generates a new operator identifier.
   */
  def generateId = Identifier("unnamed_" + lastId.incrementAndGet())

  /**
   * Reads the operator identifier from an xml element.
   */
  def readId(xml : Node) : Identifier = {
    (xml \ "@id").headOption.map(_.text).map(Identifier(_)).getOrElse(generateId)
  }
}