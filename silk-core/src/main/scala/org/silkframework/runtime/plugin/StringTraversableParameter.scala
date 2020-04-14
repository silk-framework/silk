package org.silkframework.runtime.plugin

import scala.language.implicitConversions

/**
  * A string representation of a Traversable[String]
  */
case class StringTraversableParameter(value: Traversable[String])

object StringTraversableParameter {
  implicit def fromStringTraversable(i: Traversable[String]): StringTraversableParameter = {
    StringTraversableParameter(i)
  }

  implicit def toStringTraversable(i: StringTraversableParameter): Traversable[String] = i.value
}
