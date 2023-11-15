package org.silkframework.runtime.plugin.types

import scala.language.implicitConversions

/**
  * A string representation of a Traversable[String]
  */
case class StringTraversableParameter(value: Iterable[String])

object StringTraversableParameter {
  implicit def fromStringTraversable(i: Iterable[String]): StringTraversableParameter = {
    StringTraversableParameter(i)
  }

  implicit def toStringTraversable(i: StringTraversableParameter): Iterable[String] = i.value
}
