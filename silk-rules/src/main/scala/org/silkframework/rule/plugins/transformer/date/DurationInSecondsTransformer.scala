package org.silkframework.rule.plugins.transformer.date

import java.util.Date

import javax.xml.datatype.DatatypeFactory
import org.silkframework.rule.input.SimpleTransformer
import org.silkframework.runtime.plugin.annotations.{Plugin, PluginReference}

@Plugin(
  id = DurationInSecondsTransformer.pluginId,
  categories = Array("Date"),
  label = "Duration in seconds",
  description = "Converts an xsd:duration to seconds.",
  relatedPlugins = Array(
    new PluginReference(
      id = NumberToDurationTransformer.pluginId,
      description = "Duration in seconds outputs a second count; Number to duration consumes one. Configured for seconds, Number to duration is the write operation to Duration in seconds' read."
    )
  )
)
case class DurationInSecondsTransformer() extends SimpleTransformer {

  private val datatypeFactory = DatatypeFactory.newInstance()

  override def evaluate(value: String): String = {
    val milliseconds = datatypeFactory.newDuration(value).getTimeInMillis(new Date())
    val seconds = milliseconds / 1000.0
    seconds.toString
  }
}

object DurationInSecondsTransformer {
  final val pluginId = "durationInSeconds"
}