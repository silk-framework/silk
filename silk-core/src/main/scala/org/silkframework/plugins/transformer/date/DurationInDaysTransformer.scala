package org.silkframework.plugins.transformer.date

import java.util.Date
import javax.xml.datatype.DatatypeFactory

import org.silkframework.rule.input.SimpleTransformer
import org.silkframework.runtime.plugin.Plugin

@Plugin(
   id = "durationInDays",
   categories = Array("Date"),
   label = "Duration in Days",
   description = "Converts an xsd:duration to days."
 )
case class DurationInDaysTransformer() extends SimpleTransformer {

   private val datatypeFactory = DatatypeFactory.newInstance()

   override def evaluate(value: String) = {
     val milliseconds = datatypeFactory.newDuration(value).getTimeInMillis(new Date())
     val days = milliseconds / 1000.0 / 60.0 / 60.0 / 24.0
     days.toString
   }
 }