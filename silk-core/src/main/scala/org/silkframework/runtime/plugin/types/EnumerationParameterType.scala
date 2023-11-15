package org.silkframework.runtime.plugin.types

/**
  * A trait for enumeration parameters with some additional generic methods.
  */
trait EnumerationParameterType {
  def id: String

  def displayName: String
}
