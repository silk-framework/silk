package org.silkframework.preprocessing.transformer

import scala.xml.Node

/**
 * Represents a Transformer
 *
 */
trait Transformer{
  def apply(values: List[String]): List[String]
}

