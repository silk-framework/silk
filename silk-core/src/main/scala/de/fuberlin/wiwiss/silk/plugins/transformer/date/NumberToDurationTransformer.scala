package de.fuberlin.wiwiss.silk.plugins.transformer.date

import javax.xml.datatype.DatatypeFactory

import de.fuberlin.wiwiss.silk.rule.input.SimpleTransformer
import de.fuberlin.wiwiss.silk.runtime.plugin.Plugin
import de.fuberlin.wiwiss.silk.util.StringUtils.DoubleLiteral

@Plugin(
   id = "numberToDuration",
   categories = Array("Date"),
   label = "Number to Duration",
   description = "Converts a number to an xsd:duration. The base unit may be one of the following: 'day', 'month', 'year'."
 )
case class NumberToDurationTransformer(unit: String = "day") extends SimpleTransformer {

   private val datatypeFactory = DatatypeFactory.newInstance()

   override def evaluate(value: String) = {
     val number = value match { case DoubleLiteral(d) => d }
     val duration = unit match {
       case "day" => datatypeFactory.newDuration((number * 24 * 60 * 60 * 1000).toLong)
       case "month" => datatypeFactory.newDurationYearMonth(true, 0, number.toInt)
       case "year" => datatypeFactory.newDurationYearMonth(true, number.toInt, 0)
     }
     duration.toString
   }
 }