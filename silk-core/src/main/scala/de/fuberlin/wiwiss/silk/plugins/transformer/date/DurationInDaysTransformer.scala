package de.fuberlin.wiwiss.silk.plugins.transformer.date

import de.fuberlin.wiwiss.silk.runtime.plugin.Plugin
import de.fuberlin.wiwiss.silk.linkagerule.input.SimpleTransformer
import javax.xml.datatype.DatatypeFactory
import java.util.Date

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