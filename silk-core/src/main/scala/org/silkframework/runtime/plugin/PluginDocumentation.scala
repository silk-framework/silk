package org.silkframework.runtime.plugin

import org.silkframework.config.Prefixes
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
    val categories = PluginRegistry.availablePlugins[T].flatMap(getCategories).distinct.sorted
    for (category <- categories) {
      if (categories.size > 1)
        sb ++= "### " + category.getOrElse("Uncategorized") + "\n\n"
      for (cat <- category; categoryDescription <- categoryDescriptions.get(cat)) {
        sb ++= categoryDescription + "\n\n"
      }
      pluginCategory[T](title, category, pluginParameterDisplay)
    }
  }

  private def getCategories(pluginDesc: PluginDescription[_]): Seq[Option[String]] = {
    val categories = pluginDesc.categories.filter(_ != PluginCategories.recommended)
    if(categories.isEmpty) {
      Seq(None)
    } else {
      categories.map(Some(_))
    }
  }

  def pluginCategory[T: ClassTag](title: String,
                                  category: Option[String],
                                  pluginParameterDisplay: PluginParameterDisplay)
                                 (implicit sb: StringBuilder): Unit = {
    val plugins = category match {
      case Some(cat) =>
        PluginRegistry.availablePlugins[T].filter(_.categories.contains(cat)).sortBy(_.id.toString)
      case None =>
        PluginRegistry.availablePlugins[T].sortBy(_.id.toString)
    }
    for (plugin <- plugins) {
      val paramTable =
        Table(
          name = title,
          header = pluginParameterDisplay.headers,
          values = plugin.parameters.map(p => p.name +: pluginParameterDisplay.generateValues(p))
        )(columnWidthInCharacters = pluginParameterDisplay.maxCharsInColumns)
      serializeToMarkdown(plugin, paramTable)
    }
  }

  def serializeToMarkdown[T](plugin: PluginDescription[T],
                             table: Table)
                            (implicit sb: StringBuilder): Unit = {
    sb ++= "#### " + plugin.label + "\n\n"
    sb ++= plugin.description + "\n\n"
    if (table.values.nonEmpty) {
      sb ++= table.toMarkdown() + "\n"
    } else {
      sb ++= "This plugin does not require any parameters.\n"
    }
    sb ++= s"The identifier for this plugin is `${plugin.id}`.\n\n"
    sb ++= s"It can be found in the package `${plugin.pluginClass.getPackage.getName}`.\n\n"
    sb ++= plugin.documentation + "\n\n"
  }

  def formatDefaultValue(parameter: PluginParameter): String = {
    val paramType = parameter.parameterType.asInstanceOf[ParameterType[AnyRef]]
    parameter.defaultValue match {
      case Some(v) if v == null => "*null*"
      case Some(v) if v == "" => "*empty string*"
      case Some(v) => paramType.toString(v)(Prefixes.empty)
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

case class PluginParameterDisplay(headers: Seq[String],
                                  generateValues: PluginParameter => Seq[String],
                                  maxCharsInColumns: Seq[Int])
