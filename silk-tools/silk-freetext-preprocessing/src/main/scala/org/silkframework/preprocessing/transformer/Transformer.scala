package org.silkframework.preprocessing.transformer

/**
 * Represents a Transformer
 *
 */
trait Transformer{
  def apply(values: List[String]): List[String]
}

