package org.silkframework.rule.plugins.transformer.date

import java.util.Date

import javax.xml.datatype.DatatypeFactory
import org.silkframework.rule.input.SimpleTransformer
import org.silkframework.runtime.plugin.annotations.{Plugin, PluginReference}

@Plugin(
   id = DurationInYearsTransformer.pluginId,
   categories = Array("Date"),
   label = "Duration in years",
   description = "Converts an xsd:duration to years.",
   relatedPlugins = Array(
     new PluginReference(
       id = NumberToDurationTransformer.pluginId,
       description = "Duration in years reduces a duration to a plain year count. Number to duration goes the other direction: it builds a duration from a year count when configured for years."
     )
   )
 )
case class DurationInYearsTransformer() extends SimpleTransformer {

   private val datatypeFactory = DatatypeFactory.newInstance()

   override def evaluate(value: String): String = {
     val milliseconds = datatypeFactory.newDuration(value).getTimeInMillis(new Date())
     val days = milliseconds / 1000.0 / 60.0 / 60.0 / 24.0
     val years = days / 365.25
     years.toString
   }
 }

object DurationInYearsTransformer {
  final val pluginId = "durationInYears"
}
