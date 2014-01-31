package de.fuberlin.wiwiss.silk.preprocessing.transformer

import scala.xml.Node

/**
 * Created by Petar on 28/01/14.
 */
trait Transformer{
  def apply(values: List[String]): List[String]
}

