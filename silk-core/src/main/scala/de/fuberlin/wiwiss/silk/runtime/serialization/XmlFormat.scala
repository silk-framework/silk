package de.fuberlin.wiwiss.silk.runtime.serialization

import scala.xml.Node

trait XmlFormat[T] {

  def read(node: Node): T

  def write(value: T): Node
}