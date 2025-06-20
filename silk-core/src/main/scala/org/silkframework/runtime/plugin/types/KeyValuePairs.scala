package org.silkframework.runtime.plugin.types

import org.silkframework.runtime.plugin.{PluginContext, StringParameterType}
import org.snakeyaml.engine.v2.api.{Dump, DumpSettings, Load, LoadSettings}
import org.snakeyaml.engine.v2.common.FlowStyle

import scala.jdk.CollectionConverters.{MapHasAsJava, MapHasAsScala}

case class KeyValuePairs(values: Map[String, String])

object KeyValuePairsType extends StringParameterType[KeyValuePairs] {

  override def name: String = "keyValuePairs"

  override def description: String = "YAML key-value pairs"

  def fromString(str: String)(implicit context: PluginContext): KeyValuePairs = {
    val settings = LoadSettings.builder.build
    val load = new Load(settings)
    val values = load.loadFromString(str) match {
      case map: java.util.Map[_, _] =>
        map.asScala.map { case (k, v) => k.toString -> v.toString }.toMap
      case _ =>
        throw new IllegalArgumentException(s"Expected a map, but got: $str")
    }
    KeyValuePairs(values)
  }

  override def toString(value: KeyValuePairs)(implicit pluginContext: PluginContext): String = {
    val settings = DumpSettings.builder.setDefaultFlowStyle(FlowStyle.BLOCK).build
    val dump = new Dump(settings)
    val str = dump.dumpToString(value.values.asJava)
    str
  }

}