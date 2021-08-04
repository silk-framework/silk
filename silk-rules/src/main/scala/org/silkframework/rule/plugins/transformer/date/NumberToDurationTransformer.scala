package org.silkframework.rule.plugins.transformer.date

import javax.xml.datatype.DatatypeFactory
import org.silkframework.rule.input.SimpleTransformer
import org.silkframework.util.StringUtils.DoubleLiteral
import NumberToDurationTransformer._
import org.silkframework.runtime.plugin.annotations.Plugin

@Plugin(
   id = "numberToDuration",
   categories = Array("Date"),
   label = "Number to duration",
   description = "Converts a number to an xsd:duration."
 )
case class NumberToDurationTransformer(unit: DateUnit = DateUnit.day) extends SimpleTransformer {

  override def evaluate(value: String): String = {
    val number = value match { case DoubleLiteral(d) => d }
    val duration = unit match {
      case DateUnit.milliseconds => datatypeFactory.newDuration(number.toLong)
      case DateUnit.seconds =>  datatypeFactory.newDuration((number * 1000).toLong)
      case DateUnit.day => datatypeFactory.newDuration((number * 24 * 60 * 60 * 1000).toLong)
      case DateUnit.month => datatypeFactory.newDurationYearMonth(true, 0, number.toInt)
      case DateUnit.year => datatypeFactory.newDurationYearMonth(true, number.toInt, 0)
    }
    duration.toString
  }
}

object NumberToDurationTransformer {
  private val datatypeFactory = DatatypeFactory.newInstance()
}
