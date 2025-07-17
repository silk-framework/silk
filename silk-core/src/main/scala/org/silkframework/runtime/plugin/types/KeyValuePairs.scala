package org.silkframework.runtime.plugin.types

import org.silkframework.runtime.plugin.{PluginContext, StringParameterType}
import org.snakeyaml.engine.v2.api.{Dump, DumpSettings, Load, LoadSettings}
import org.snakeyaml.engine.v2.common.FlowStyle

import scala.collection.immutable.ListMap
import scala.jdk.CollectionConverters.MapHasAsJava

case class KeyValuePairs(values: ListMap[String, String])

object KeyValuePairs {
  def empty: KeyValuePairs = KeyValuePairs(ListMap.empty[String, String])
}

object KeyValuePairsType extends StringParameterType[KeyValuePairs] {

  override def name: String = "keyValuePairs"

  override def description: String = "YAML key-value pairs"

  def fromString(str: String)(implicit context: PluginContext): KeyValuePairs = {
    if(str.trim.isEmpty) {
      KeyValuePairs.empty
    } else {
      val settings = LoadSettings.builder.build
      val load = new Load(settings)
      val values = load.loadFromString(str) match {
        case map: java.util.Map[_, _] =>
          var keyValues = ListMap.empty[String, String]
          map.forEach((k, v) => {
            keyValues = keyValues.updated(k.toString, v.toString)
          })
          keyValues
        case _ =>
          throw new IllegalArgumentException(s"Expected a map, but got: $str")
      }
      KeyValuePairs(values)
    }
  }

  override def toString(value: KeyValuePairs)(implicit pluginContext: PluginContext): String = {
    if(value.values.isEmpty) {
      ""
    } else {
      val settings = DumpSettings.builder.setDefaultFlowStyle(FlowStyle.BLOCK).build
      val dump = new Dump(settings)
      val str = dump.dumpToString(value.values.asJava)
      str
    }
  }

}