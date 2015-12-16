package org.silkframework.plugins

import java.io.{OutputStreamWriter, FileOutputStream}
import org.silkframework.rule.input.Transformer
import org.silkframework.rule.similarity.DistanceMeasure
import org.silkframework.runtime.plugin._
import org.silkframework.util.Table

import scala.reflect.ClassTag

/**
 * Script for generating markdown documentation for all registered plugins.
 */
object PluginDocumentation extends App {

  val sb = new StringBuilder

  printPlugins[DistanceMeasure](
    title = "Similarity Measures",
    description = "The following similarity measures are included:"
  )

  printPlugins[Transformer](
    title = "Transformations",
    description = "The following transform and normalization functions are included:"
  )

  val writer = new OutputStreamWriter(new FileOutputStream("doc/plugins2.md"))
  writer.write(sb.toString())
  writer.close()

  def printPlugins[T: ClassTag](title: String, description: String) = {
    sb ++= "# " + title + "\n\n"
    sb ++= description + "\n\n"
    for(category <- PluginRegistry.availablePlugins[T].flatMap(_.categories).distinct if category != "Recommended") {
      sb ++= "## " + category + "\n"
      sb ++= pluginTable[T](title, category).toMarkdown
      sb ++= "\n"
    }
  }

  def pluginTable[T: ClassTag](title: String, category: String) = {
    val plugins = PluginRegistry.availablePlugins[T].filter(_.categories.contains(category)).sortBy(_.id.toString)
    Table(
      name = title,
      header = Seq("Function and parameters", "Name", "Description"),
      rows = plugins.map(formatFunction),
      values = for(plugin <- plugins) yield Seq(plugin.label, plugin.description)
    )
  }

  def formatFunction(plugin: PluginDescription[_]): String = {
    plugin.id.toString + plugin.parameters.map(formatParameter).mkString("(", ", ", ")")
  }

  def formatParameter(parameter: Parameter): String = {
    val signature = parameter.name + ": " + parameter.dataType.toString
    parameter.defaultValue match {
      case Some(default) => s"[$signature = '$default']"
      case None => signature
    }
  }
}
