package org.silkframework.runtime.plugin.types

import org.silkframework.runtime.plugin.{PluginContext, StringParameterType}

case class KeyValuePairs(str: String) {

}

object KeyValuePairsType extends StringParameterType[KeyValuePairs] {

  override def name: String = "keyValuePairs"

  override def description: String = "TODO"

  def fromString(str: String)(implicit context: PluginContext): KeyValuePairs = {
    KeyValuePairs(str)
  }

  override def toString(value: KeyValuePairs)(implicit pluginContext: PluginContext): String = {
    value.str
  }

}
