package org.silkframework.plugins.transformer.date

import java.util.Date
import javax.xml.datatype.DatatypeFactory

import org.silkframework.rule.input.SimpleTransformer
import org.silkframework.runtime.plugin.Plugin

@Plugin(
  id = "durationInSeconds",
  categories = Array("Date"),
  label = "Duration in Seconds",
  description = "Converts an xsd:duration to seconds."
)
case class DurationInSecondsTransformer() extends SimpleTransformer {

  private val datatypeFactory = DatatypeFactory.newInstance()

  override def evaluate(value: String) = {
    val milliseconds = datatypeFactory.newDuration(value).getTimeInMillis(new Date())
    val seconds = milliseconds / 1000.0
    seconds.toString
  }
}