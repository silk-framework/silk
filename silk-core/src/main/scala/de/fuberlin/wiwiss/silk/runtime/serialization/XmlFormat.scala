package de.fuberlin.wiwiss.silk.runtime.serialization

import scala.xml.Node

/**
 * XML serialization format.
 */
trait XmlFormat[T] {

  /**
   * Deserialize a value from XML.
   */
  def read(node: Node): T

  /**
   * Serialize a value to XML.
   */
  def write(value: T): Node
}