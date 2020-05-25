package org.silkframework.rule.plugins.transformer.date

import java.util.GregorianCalendar

import javax.xml.datatype.DatatypeFactory
import org.silkframework.rule.plugins.transformer.date.CurrentDateTransformer._
import org.silkframework.rule.input.Transformer
import org.silkframework.runtime.plugin.annotations.Plugin

@Plugin(
  id = "currentDate",
  label = "Current date",
  categories = Array("Date", "Value"),
  description = "Outputs the current date."
)
case class CurrentDateTransformer() extends Transformer {

  override def apply(values: Seq[Seq[String]]): Seq[String] = {
    val currentDate = datatypeFactory.newXMLGregorianCalendar(new GregorianCalendar).toXMLFormat
    Seq(currentDate)
  }
}

object CurrentDateTransformer {

  private val datatypeFactory = DatatypeFactory.newInstance()

}