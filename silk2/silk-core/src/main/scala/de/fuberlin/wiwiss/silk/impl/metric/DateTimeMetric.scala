package de.fuberlin.wiwiss.silk.impl.metric

import scala.math._
import javax.xml.datatype.{DatatypeConstants, XMLGregorianCalendar, DatatypeFactory}
import de.fuberlin.wiwiss.silk.util.strategy.StrategyAnnotation
import de.fuberlin.wiwiss.silk.linkspec.condition.SimpleSimilarityMeasure

@StrategyAnnotation(
  id = "dateTime",
  label = "DateTime",
  description = "Computes the similarity between two date time values (xsd:dateTime format). " +
                "At a difference of 'maxSeconds', the metric evaluates to 0 and progresses towards 1 with a lower difference.")
class DateTimeMetric(maxSeconds : Int) extends SimpleSimilarityMeasure
{
  override def evaluate(str1 : String, str2 : String, threshold : Double) =
  {
    try
    {
      val datatypeFactory = DatatypeFactory.newInstance

      val date1 = datatypeFactory.newXMLGregorianCalendar(str1)
      val date2 = datatypeFactory.newXMLGregorianCalendar(str2)

      val seconds = abs(totalSeconds(date1) - totalSeconds(date2))

      max(1.0 - seconds.toDouble / maxSeconds.toDouble, 0.0)
    }
    catch
    {
      case ex : IllegalArgumentException => 0.0
    }
  }

  private def totalSeconds(date : XMLGregorianCalendar) =
  {
    val seconds = date.getSecond match
    {
      case DatatypeConstants.FIELD_UNDEFINED => 0
      case s => s
    }

    val minuteSeconds = date.getMinute match
    {
      case DatatypeConstants.FIELD_UNDEFINED => 0
      case m => m * 60
    }

    val hourSeconds = date.getHour match
    {
      case DatatypeConstants.FIELD_UNDEFINED => 0
      case h => h * 60 * 60
    }

    val daySeconds = date.getDay match
    {
      case DatatypeConstants.FIELD_UNDEFINED => 0
      case d => d * 24 * 60 * 60
    }

    val monthSeconds = date.getMonth match
    {
      case DatatypeConstants.FIELD_UNDEFINED => 0
      case m => m * 30 * 24 * 60 * 60
    }

    val yearSeconds = date.getYear match
    {
      case DatatypeConstants.FIELD_UNDEFINED => 0
      case y => y * 365 * 24 * 60 * 60
    }

    seconds + minuteSeconds + hourSeconds + daySeconds + monthSeconds + yearSeconds
  }
}