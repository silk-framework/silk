package de.fuberlin.wiwiss.silk.impl.metric

import de.fuberlin.wiwiss.silk.linkspec.Metric
import de.fuberlin.wiwiss.silk.util.StringUtils._
import javax.xml.datatype.DatatypeFactory
import scala.math._

class DateMetric(val params : Map[String, String]) extends Metric
{
    private val maxDays = params.get("max_days") match
    {
        case Some(IntLiteral(days)) => days
        case _ => throw new IllegalArgumentException("Integral parameter 'max_days' required")
    }

    override def evaluate(str1 : String, str2 : String) =
    {
        try
        {
            val datatypeFactory = DatatypeFactory.newInstance

            val date1 = datatypeFactory.newXMLGregorianCalendar(str1)
            val date2 = datatypeFactory.newXMLGregorianCalendar(str2)

            val numDays1 = date1.getDay + date1.getMonth * 30 + date1.getYear * 365
            val numDays2 = date2.getDay + date2.getMonth * 30 + date2.getYear * 365

            val days = abs(numDays1 - numDays2)

            max(1.0 - days.toDouble / maxDays.toDouble, 0.0)
        }
        catch
        {
            case ex : IllegalArgumentException => 0.0
        }
    }
}
