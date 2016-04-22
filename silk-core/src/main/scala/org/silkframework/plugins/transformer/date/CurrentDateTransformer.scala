package org.silkframework.plugins.transformer.date

import java.util.GregorianCalendar
import javax.xml.datatype.DatatypeFactory

import org.silkframework.plugins.transformer.date.CurrentDateTransformer._
import org.silkframework.rule.input.Transformer
import org.silkframework.runtime.plugin.Plugin

@Plugin(
  id = "currentDate",
  label = "current date",
  categories = Array("Date", "Value"),
  description = "The current date."
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