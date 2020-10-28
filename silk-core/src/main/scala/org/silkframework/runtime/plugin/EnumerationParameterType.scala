package org.silkframework.runtime.plugin

import java.util.Locale

/**
  * A trait for enumeration parameters with some additional generic methods.
  */
trait EnumerationParameterType {

  def id: String

  def displayName: String

  def matchesId(str: String): Boolean = {
    val normalizedValue = str.trim.toLowerCase(Locale.ROOT)

    id.toLowerCase(Locale.ROOT) == normalizedValue ||
    displayName.toLowerCase(Locale.ROOT) == normalizedValue
  }
}
