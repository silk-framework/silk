package org.silkframework.rule.plugins.transformer.date

import java.util.Date

import javax.xml.datatype.DatatypeFactory
import org.silkframework.rule.input.SimpleTransformer
import org.silkframework.runtime.plugin.annotations.{Plugin, PluginReference}

@Plugin(
   id = DurationInDaysTransformer.pluginId,
   categories = Array("Date"),
   label = "Duration in days",
   description = "Converts an xsd:duration to days.",
   relatedPlugins = Array(
     new PluginReference(
       id = NumberToDurationTransformer.pluginId,
       description = "Duration in days extracts a day count from a duration. Number to duration is the reverse: it builds a duration from a number, and days is its default unit."
     )
   )
 )
case class DurationInDaysTransformer() extends SimpleTransformer {

   private val datatypeFactory = DatatypeFactory.newInstance()

   override def evaluate(value: String): String = {
     val milliseconds = datatypeFactory.newDuration(value).getTimeInMillis(new Date())
     val days = milliseconds / 1000.0 / 60.0 / 60.0 / 24.0
     days.toString
   }
 }

object DurationInDaysTransformer {
  final val pluginId = "durationInDays"
}
