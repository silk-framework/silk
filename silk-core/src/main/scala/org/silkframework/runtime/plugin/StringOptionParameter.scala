package org.silkframework.runtime.plugin

/**
  * An optional string. Empty strings are interpreted as missing value.
  */
case class StringOptionParameter(value: Option[String])

object StringOptionParameter {
  implicit def toStringOptionParameter(v: Option[String]): StringOptionParameter = StringOptionParameter(v)
  implicit def fromStringOptionParameter(v: StringOptionParameter): Option[String] = v.value
}
