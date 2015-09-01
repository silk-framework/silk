package de.fuberlin.wiwiss.silk.plugins

import java.io.{OutputStreamWriter, FileOutputStream}

import de.fuberlin.wiwiss.silk.linkagerule.input.Transformer
import de.fuberlin.wiwiss.silk.linkagerule.similarity.DistanceMeasure
import de.fuberlin.wiwiss.silk.runtime.plugin.{AnyPlugin, Parameter, PluginDescription, PluginFactory}
import de.fuberlin.wiwiss.silk.util.Table

/**
 * Script for generating documentation about plugins.
 * Currently generates textile documentation for transformations.
 */
object PluginDocumentation extends App {

  CorePlugins.register()

  val sb = new StringBuilder

  printPlugins(
    title = "Similarity Measures",
    description = "The following similarity measures are included:",
    pluginFactory = DistanceMeasure
  )

  printPlugins(
    title = "Transformations",
    description = "The following transform and normalization functions are included:",
    pluginFactory = Transformer
  )

  val writer = new OutputStreamWriter(new FileOutputStream("../doc/plugins.md"))
  writer.write(sb.toString())
  writer.close()

  def printPlugins(title: String, description: String, pluginFactory: PluginFactory[_ <: AnyPlugin]) = {
    sb ++= "# " + title + "\n\n"
    sb ++= description + "\n"
    for(category <- pluginFactory.availablePlugins.flatMap(_.categories).distinct if category != "Recommended") {
      sb ++= "## " + category + "\n"
      sb ++= pluginTable(pluginFactory, category).toMarkdown
    }
  }

  def pluginTable(pluginFactory: PluginFactory[_ <: AnyPlugin], category: String) = {
    val plugins = pluginFactory.availablePlugins.filter(_.categories.contains(category)).sortBy(_.id.toString)
    Table(
      name = pluginFactory.getClass.getSimpleName,
      header = Seq("Function and parameters", "Name", "Description"),
      rows = plugins.map(formatFunction),
      values = for(plugin <- plugins) yield Seq(plugin.label, plugin.description)
    )
  }

  def formatFunction(plugin: PluginDescription[_]) = {
    plugin.id + plugin.parameters.map(formatParameter).mkString("(", ", ", ")")
  }

  def formatParameter(parameter: Parameter) = {
    val signature = parameter.name + ": " + parameter.dataType.toString
    parameter.defaultValue match {
      case Some(default) => s"[$signature = '$default']"
      case None => signature
    }
  }
}
