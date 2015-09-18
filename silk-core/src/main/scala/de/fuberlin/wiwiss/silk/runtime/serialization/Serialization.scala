package de.fuberlin.wiwiss.silk.runtime.serialization

import java.io.File

import de.fuberlin.wiwiss.silk.config.Prefixes
import de.fuberlin.wiwiss.silk.runtime.resource.{EmptyResourceManager, ResourceLoader}

import scala.xml.{XML, Node}

/**
 * Serializes between classes and XML.
 * In order to be serializable a class needs to provide an implicit XmlFormat object.
 */
object Serialization {

  def toXml[T](value: T)(implicit format: XmlFormat[T], prefixes: Prefixes = Prefixes.empty): Node = {
    format.write(value)
  }

  def fromXml[T](node: Node)(implicit format: XmlFormat[T], prefixes: Prefixes = Prefixes.empty, resourceLoader: ResourceLoader = EmptyResourceManager): T = {
    format.read(node)
  }
}