package org.silkframework.rule.plugins.transformer.date

import java.util.GregorianCalendar

import javax.xml.datatype.DatatypeFactory
import org.silkframework.rule.plugins.transformer.date.CurrentDateTransformer._
import org.silkframework.rule.input.Transformer
import org.silkframework.runtime.plugin.annotations.{Plugin, PluginReference}

@Plugin(
  id = CurrentDateTransformer.pluginId,
  label = "Current date",
  categories = Array("Date", "Value"),
  description = "Outputs the current date.",
  relatedPlugins = Array(
    new PluginReference(
      id = ParseDateTransformer.pluginId,
      description = "Current date always outputs today's date, ignoring whatever values it receives. Parse date is input-driven: it reads a date from the input string and converts it according to a configured format."
    )
  )
)
case class CurrentDateTransformer() extends Transformer {

  override def apply(values: Seq[Seq[String]]): Seq[String] = {
    val currentDate = datatypeFactory.newXMLGregorianCalendar(new GregorianCalendar).toXMLFormat
    Seq(currentDate)
  }
}

object CurrentDateTransformer {
  final val pluginId = "currentDate"
  private val datatypeFactory = DatatypeFactory.newInstance()
}
