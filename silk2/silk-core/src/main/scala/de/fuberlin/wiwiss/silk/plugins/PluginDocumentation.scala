package de.fuberlin.wiwiss.silk.plugins

import de.fuberlin.wiwiss.silk.linkagerule.input.Transformer
import de.fuberlin.wiwiss.silk.runtime.plugin.{Parameter, PluginDescription}
import de.fuberlin.wiwiss.silk.util.Table

/**
 * Script for generating documentation about plugins.
 * Currently generates textile documentation for transformations.
 */
object PluginDocumentation extends App {

  Plugins.register()

  println("h1. Transformations")
  println()
  println("Silk provides the following transformation and normalization functions:")
  println()
  println(formatPlugins(Transformer.availablePlugins.sortBy(_.id)).toTextile)

  def formatPlugins(plugins: Seq[PluginDescription[_]]) = {
    Table(
      name = "Transformations",
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
