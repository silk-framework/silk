package de.fuberlin.wiwiss.silk.plugins.metric

import scala.math._
import javax.xml.datatype.{DatatypeConstants, XMLGregorianCalendar, DatatypeFactory}
import de.fuberlin.wiwiss.silk.util.plugin.Plugin
import de.fuberlin.wiwiss.silk.linkagerule.similarity.SimpleDistanceMeasure
import de.fuberlin.wiwiss.silk.entity.Index

@Plugin(
  id = "date",
  label = "Date",
  description = "The distance in days between two dates ('YYYY-MM-DD' format).")
case class DateMetric() extends SimpleDistanceMeasure {

  private val datatypeFactory = DatatypeFactory.newInstance

  private val minDays = 0

  private val maxDays = 3000 * 365

  override def evaluate(str1: String, str2: String, threshold: Double) = {
    try {
      val date1 = datatypeFactory.newXMLGregorianCalendar(str1)
      val date2 = datatypeFactory.newXMLGregorianCalendar(str2)

      abs(totalDays(date1) - totalDays(date2)).toDouble
    } catch {
      case ex: IllegalArgumentException => Double.PositiveInfinity
    }
  }

  override def indexValue(str: String, limit: Double): Index = {
    try {
      val date = datatypeFactory.newXMLGregorianCalendar(str)
      val days = totalDays(date)
      Index.continuous(days, minDays, maxDays, limit)
    } catch {
      case ex: IllegalArgumentException => Index.empty
    }
  }

  private def totalDays(date: XMLGregorianCalendar) = {
    val days = date.getDay match {
      case DatatypeConstants.FIELD_UNDEFINED => 0
      case d => d
    }

    val monthDays = date.getMonth match {
      case DatatypeConstants.FIELD_UNDEFINED => 0
      case m => m * 30
    }

    val yearDays = date.getYear match {
      case DatatypeConstants.FIELD_UNDEFINED => 0
      case y => y * 365
    }

    days + monthDays + yearDays
  }
}
