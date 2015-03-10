package de.fuberlin.wiwiss.silk.preprocessing.transformer

import scala.xml.Node

/**
 * Represents a Transformer
 *
 */
trait Transformer{
  def apply(values: List[String]): List[String]
}

