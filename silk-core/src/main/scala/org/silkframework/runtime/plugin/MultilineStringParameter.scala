package org.silkframework.runtime.plugin

/**
  * A multiline string parameter.
  */
case class MultilineStringParameter(str: String) {
  override def toString: String = str
}