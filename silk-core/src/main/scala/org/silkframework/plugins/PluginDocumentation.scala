package org.silkframework.plugins

import java.io.{OutputStreamWriter, FileOutputStream}
import org.silkframework.dataset.{DatasetPlugin, Dataset}
import org.silkframework.rule.input.Transformer
import org.silkframework.rule.similarity.{Aggregator, Aggregation, DistanceMeasure}
import org.silkframework.runtime.plugin._
import org.silkframework.util.Table

import scala.reflect.ClassTag

/**
 * Generates markdown documentation for all registered plugins.
 */
object PluginDocumentation {

  // Currently we add category descriptions manually here
  val categoryDescriptions: Map[String, String] =
    Map(
      "Characterbased" -> "Character-based distance measures compare strings on the character level. They are well suited for\nhandling typographical errors.",
      "Tokenbased" -> "While character-based distance measures work well for typographical errors, there are a number of tasks where token-base distance measures are better suited:\n- Strings where parts are reordered e.g. &ldquo;John Doe&rdquo; and &ldquo;Doe, John&rdquo;\n- Texts consisting of multiple words"
    )

  def apply(): String = {
    implicit val sb = new StringBuilder

    sb ++= "# Plugin Reference\n\n"

    plugins[DatasetPlugin](
      title = "Dataset Plugins",
      description = "The following dataset plugins are available:"
    )

    plugins[DistanceMeasure](
      title = "Similarity Measures",
      description = "The following similarity measures are available:"
    )

    plugins[Transformer](
      title = "Transformations",
      description = "The following transform and normalization functions are available:"
    )

    plugins[Aggregator](
      title = "Aggregations",
      description = "The following aggregation functions are available:"
    )

    sb.toString
  }

  def plugins[T: ClassTag](title: String, description: String)(implicit sb: StringBuilder): Unit = {
    sb ++= "## " + title + "\n\n"
    sb ++= description + "\n\n"
    val categories = PluginRegistry.availablePlugins[T].flatMap(_.categories).filter(_ != "Recommended").distinct.sorted
    for(category <- categories) {
      if(categories.size > 1)
        sb ++= "### " + category + "\n\n"
      for(categoryDescription <- categoryDescriptions.get(category)) {
        sb ++= categoryDescription + "\n\n"
      }
      pluginCategory[T](title, category)
    }
  }

  def pluginCategory[T: ClassTag](title: String, category: String)(implicit sb: StringBuilder): Unit = {
    val plugins = PluginRegistry.availablePlugins[T].filter(_.categories.contains(category)).sortBy(_.id.toString)
    for(plugin <- plugins) {
      sb ++= "#### " + plugin.label + "\n\n"
      sb ++= plugin.description + "\n\n"
      val paramTable =
        Table(
          name = title,
          header = Seq("Parameter", "Type", "Default"),
          rows = plugin.parameters.map(_.name),
          values = plugin.parameters.map(p => Seq(p.dataType.toString, formatDefaultValue(p.defaultValue)))
        )
      if(paramTable.rows.nonEmpty)
        sb ++= paramTable.toMarkdown + "\n"
      else
        sb ++= "This plugin does not require any parameters.\n\n"
    }
  }

  def formatDefaultValue(value: Option[AnyRef]): String = {
    value match {
      case Some(v) if v == null => "*null*"
      case Some(v) if v == "" => "*empty string*"
      case Some(v) => v.toString
      case None => "*no default*"
    }
  }
}
