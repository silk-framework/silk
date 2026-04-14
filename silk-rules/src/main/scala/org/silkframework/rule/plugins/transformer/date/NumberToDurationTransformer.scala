package org.silkframework.rule.plugins.transformer.date

import javax.xml.datatype.DatatypeFactory
import org.silkframework.rule.input.SimpleTransformer
import org.silkframework.util.StringUtils.DoubleLiteral
import NumberToDurationTransformer._
import org.silkframework.runtime.plugin.annotations.{Plugin, PluginReference}

@Plugin(
   id = NumberToDurationTransformer.pluginId,
   categories = Array("Date"),
   label = "Number to duration",
   description = "Converts a number to an xsd:duration.",
   relatedPlugins = Array(
     new PluginReference(
       id = DurationInDaysTransformer.pluginId,
       description = "Number to duration and Duration in days form a round-trip: one builds a duration from days, the other extracts days from a duration."
     ),
     new PluginReference(
       id = DurationInSecondsTransformer.pluginId,
       description = "Duration in seconds produces the finest-grained numeric output among the duration converters. Number to duration is its reverse when configured for seconds: it constructs a duration from a second count."
     ),
     new PluginReference(
       id = DurationInYearsTransformer.pluginId,
       description = "Duration in years extracts a year count from a duration value. Number to duration reconstructs the duration from that count."
     ),
     new PluginReference(
       id = DurationTransformer.pluginId,
       description = "The two plugins produce durations by different means. Duration measures the gap between a start and an end date; Number to duration builds one from a time span expressed as a number."
     )
   )
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
  final val pluginId = "numberToDuration"
  private val datatypeFactory = DatatypeFactory.newInstance()
}
