package de.fuberlin.wiwiss.silk.plugins.transformer.date

import de.fuberlin.wiwiss.silk.runtime.plugin.Plugin
import de.fuberlin.wiwiss.silk.linkagerule.input.SimpleTransformer
import javax.xml.datatype.DatatypeFactory
import java.util.{Date, Calendar}

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