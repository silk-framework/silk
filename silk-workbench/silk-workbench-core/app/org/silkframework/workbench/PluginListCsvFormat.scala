package org.silkframework.workbench

import org.silkframework.runtime.plugin.{Parameter, PluginList}
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}

case class PluginListCsvFormat() extends CsvFormat[PluginList] {

  private val sep = ';'

  private val arraySep = ','

  override def read(value: String)(implicit readContext: ReadContext): PluginList = {
    throw new UnsupportedOperationException("Reading PluginList from CSV is not supported.")
  }

  override def write(plugins: PluginList)(implicit writeContext: WriteContext[String]): String = {
    val sb = new StringBuilder()

    sb ++= s"Identifier${sep}Label${sep}Description${sep}Parameters${sep}Categories${sep}Namespace${sep}Plugin Type\n"

    for((pluginType, plugins) <- plugins.pluginsByType; plugin <- plugins) {
      sb ++= plugin.id
      sb += sep
      sb ++= escape(plugin.label)
      sb += sep
      sb ++= escape(plugin.description)
      sb += sep
      sb ++= escape(plugin.parameters.map(serializeParameter).mkString("\n"))
      sb += sep
      sb ++= escape(plugin.categories)
      sb += sep
      sb ++= plugin.pluginClass.getPackage.getName
      sb += sep
      sb ++= pluginType
      sb ++= "\n"
    }

    sb.toString()
  }

  private def serializeParameter(param: Parameter): String = {
    val sb = new StringBuilder()
    sb ++= param.label
    sb ++= ": "
    sb ++= param.description
    sb.toString
  }


  def escape(value: String): String = {
    "\"" + value.replace("\"", "\"\"") + "\""
  }

  def escape(values: Traversable[String]): String = {
    escape(values.mkString(arraySep.toString))
  }
}
