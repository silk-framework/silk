package org.silkframework.runtime.plugin

import org.silkframework.util.Identifier

import scala.language.implicitConversions

/**
  * A plugin parameter encapsulating an Option[Int].
  * The concrete type of the type parameter of an Option[A] does not seem to be accessible via reflection, so this seems necessary.
  */
case class IntOptionParameter(value: Option[Int])

object IntOptionParameter {
  implicit def toIntOptionParameter(v: Option[Int]): IntOptionParameter = IntOptionParameter(v)
  implicit def fromIntOptionParameter(v: IntOptionParameter): Option[Int] = v.value
}


/**
  * A plugin parameter encapsulating an Option[Identifier].
  * The concrete type of the type parameter of an Option[A] does not seem to be accessible via reflection, so this seems necessary.
  */
case class IdentifierOptionParameter(value: Option[Identifier])

object IdentifierOptionParameter {
  implicit def toIdentifierOptionParameter(v: Option[Identifier]): IdentifierOptionParameter = IdentifierOptionParameter(v)
  implicit def fromIdentifierOptionParameter(v: IdentifierOptionParameter): Option[Identifier] = v.value
}
