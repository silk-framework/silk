package org.silkframework.runtime.serialization

import org.silkframework.config.Prefixes
import org.silkframework.runtime.resource.{ResourceManager, EmptyResourceManager}

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