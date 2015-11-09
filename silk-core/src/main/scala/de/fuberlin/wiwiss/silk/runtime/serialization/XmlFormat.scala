package de.fuberlin.wiwiss.silk.runtime.serialization

import de.fuberlin.wiwiss.silk.config.Prefixes
import de.fuberlin.wiwiss.silk.runtime.resource.{ResourceManager, EmptyResourceManager}

import scala.xml.Node

/**
 * XML serialization format.
 */
trait XmlFormat[T] {

  /**
   * Deserialize a value from XML.
   */
  def read(node: Node)(implicit prefixes: Prefixes = Prefixes.empty, resources: ResourceManager = EmptyResourceManager): T

  /**
   * Serialize a value to XML.
   */
  def write(value: T)(implicit prefixes: Prefixes = Prefixes.empty): Node
}