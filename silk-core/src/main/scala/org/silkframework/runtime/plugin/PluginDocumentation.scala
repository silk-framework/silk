package org.silkframework.runtime.plugin

import org.silkframework.util.Table

import scala.reflect.ClassTag

/**
 * Generates markdown documentation for all registered plugins.
 */
object PluginDocumentation {

  // Currently we add category descriptions manually here
  val categoryDescriptions: Map[String, String] =
    Map(
      "Characterbased" -> "Character-based distance measures compare strings on the character level. They are well suited for handling typographical errors.",
      "Tokenbased" -> "While character-based distance measures work well for typographical errors, there are a number of tasks where token-base distance measures are better suited:\n\n- Strings where parts are reordered e.g. &ldquo;John Doe&rdquo; and &ldquo;Doe, John&rdquo;\n- Texts consisting of multiple words"
    )


  def plugins[T: ClassTag](title: String,
                           description: String)
                          (implicit sb: StringBuilder,
                           pluginParameterDisplay: PluginParameterDisplay): Unit = {
    sb ++= "## " + title + "\n\n"
    sb ++= description + "\n\n"
    val categories = PluginRegistry.availablePlugins[T].flatMap(_.categories).filter(_ != "Recommended").distinct.sorted
    for (category <- categories) {
      if (categories.size > 1)
        sb ++= "### " + category + "\n\n"
      for (categoryDescription <- categoryDescriptions.get(category)) {
        sb ++= categoryDescription + "\n\n"
      }
      pluginCategory[T](title, category, pluginParameterDisplay)
    }
  }

  def pluginCategory[T: ClassTag](title: String,
                                  category: String,
                                  pluginParameterDisplay: PluginParameterDisplay)
                                 (implicit sb: StringBuilder): Unit = {
    val plugins = PluginRegistry.availablePlugins[T].filter(_.categories.contains(category)).sortBy(_.id.toString)
    for (plugin <- plugins) {
      val paramTable =
        Table(
          name = title,
          header = pluginParameterDisplay.headers,
          rows = plugin.parameters.map(_.name),
          values = plugin.parameters.map(pluginParameterDisplay.generateValues)
        )(columnWidthInCharacters = pluginParameterDisplay.maxCharsInColumns)
      serializeToMarkdown(plugin, paramTable)
    }
  }

  def serializeToMarkdown[T](plugin: PluginDescription[T],
                             table: Table)
                            (implicit sb: StringBuilder): Unit = {
    sb ++= "#### " + plugin.label + "\n\n"
    sb ++= plugin.description + "\n\n"
    if (table.rows.nonEmpty)
      sb ++= table.toMarkdown + "\n"
    else
      sb ++= "This plugin does not require any parameters.\n"
    sb ++= "The identifier for this plugin is: `" + plugin.id + "`.\n\n"
  }

  def formatDefaultValue(value: Option[AnyRef]): String = {
    value match {
      case Some(v) if v == null => "*null*"
      case Some(v) if v == "" => "*empty string*"
      case Some(v) => v.toString
      case None => "*no default*"
    }
  }

  def formatExampleValue(value: Option[AnyRef]): String = {
    value match {
      case Some(v) if v == null => "*null*"
      case Some(v) if v == "" => "*empty string*"
      case Some(v) => v.toString
      case None => ""
    }
  }
}

case class PluginParameterDisplay
(
  headers: Seq[String],
  generateValues: Parameter => Seq[String],
  maxCharsInColumns: Seq[Int]
  )