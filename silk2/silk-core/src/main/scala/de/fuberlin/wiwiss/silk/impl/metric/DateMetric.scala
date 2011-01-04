package de.fuberlin.wiwiss.silk.impl.metric

import de.fuberlin.wiwiss.silk.linkspec.Metric
import de.fuberlin.wiwiss.silk.util.StringUtils._
import scala.math._
import javax.xml.datatype.{DatatypeConstants, XMLGregorianCalendar, DatatypeFactory}

class DateMetric(val params : Map[String, String]) extends Metric
{
  private val maxDays = params.get("max_days") match
  {
    case Some(IntLiteral(days)) => days
    case _ => throw new IllegalArgumentException("Integral parameter 'max_days' required")
  }

  override def evaluate(str1 : String, str2 : String, threshold : Double) =
  {
    try
    {
      val datatypeFactory = DatatypeFactory.newInstance

      val date1 = datatypeFactory.newXMLGregorianCalendar(str1)
      val date2 = datatypeFactory.newXMLGregorianCalendar(str2)

      val days = abs(totalDays(date1) - totalDays(date2))

      max(1.0 - days.toDouble / maxDays.toDouble, 0.0)
    }
    catch
    {
      case ex : IllegalArgumentException => 0.0
    }
  }

  private def totalDays(date : XMLGregorianCalendar) =
  {
    val days = date.getDay match
    {
      case DatatypeConstants.FIELD_UNDEFINED => 0
      case d => d
    }

    val monthDays = date.getMonth match
    {
      case DatatypeConstants.FIELD_UNDEFINED => 0
      case m => m * 30
    }

    val yearDays = date.getYear match
    {
      case DatatypeConstants.FIELD_UNDEFINED => 0
      case y => y * 365
    }

    days + monthDays + yearDays
  }
}
