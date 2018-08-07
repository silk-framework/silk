package org.silkframework.runtime.plugin

import scala.language.implicitConversions

/**
  * A multiline string parameter.
  */
case class MultilineStringParameter(str: String) {

  override def toString: String = str

  def lines: Seq[String] = str.split("[\\r\\n]+")

}

object MultilineStringParameter {
  implicit def str2MultilineString(str: String): MultilineStringParameter = MultilineStringParameter(str)
}