package org.silkframework.runtime.plugin.types

import scala.language.implicitConversions

/**
  * A string representation of an Iterable[String]
  */
case class StringIterableParameter(value: Iterable[String])

object StringIterableParameter {

  implicit def fromStringIterable(i: Iterable[String]): StringIterableParameter = {
    StringIterableParameter(i)
  }

  implicit def toStringIterable(i: StringIterableParameter): Iterable[String] = i.value
}
