package org.silkframework.plugins.transformer.date

import javax.xml.datatype.DatatypeFactory
import org.silkframework.rule.input.SimpleTransformer
import org.silkframework.runtime.plugin.Plugin
import org.silkframework.util.StringUtils.DoubleLiteral

@Plugin(
   id = "numberToDuration",
   categories = Array("Date"),
   label = "Number to Duration",
   description = "Converts a number to an xsd:duration. The base unit may be one of the following: 'day', 'month', 'year'."
 )
case class NumberToDurationTransformer(unit: DateUnit = DateUnit.day) extends SimpleTransformer {

  private val datatypeFactory = DatatypeFactory.newInstance()

  override def evaluate(value: String) = {
    val number = value match { case DoubleLiteral(d) => d }
    val duration = unit match {
      case DateUnit.day => datatypeFactory.newDuration((number * 24 * 60 * 60 * 1000).toLong)
      case DateUnit.month => datatypeFactory.newDurationYearMonth(true, 0, number.toInt)
      case DateUnit.year => datatypeFactory.newDurationYearMonth(true, number.toInt, 0)
    }
    duration.toString
  }
}
