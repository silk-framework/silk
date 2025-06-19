package controllers.sparqlapi

import scala.util.matching.Regex
import scala.language.implicitConversions

object SparqlQueryType extends Enumeration {
  case class SparqlVal(name: String, regex: Regex) extends super.Val(name)

  implicit def valueToSparqlVal(x: Value): SparqlVal = x.asInstanceOf[SparqlVal]

  val ASK         = SparqlVal("ASK", "(?s)(ask|ASK)\\s+(WHERE|where)\\s*\\{".r)
  val CONSTRUCT   = SparqlVal("CONSTRUCT", "(?s)(construct|CONSTRUCT)\\s*\\{\\.*(WHERE|where)\\s*\\{".r)
  val DESCRIBE    = SparqlVal("DESCRIBE", "(?s)(describe|DESCRIBE)\\s+".r)
  val SELECT      = SparqlVal("SELECT", "(?s)(select|SELECT)\\s+(\\?|\\*).*(WHERE|where)\\s*\\{".r)

  def determineSparqlQueryType(query: String): SparqlQueryType.Val ={
    SparqlQueryType.SELECT.regex.findFirstMatchIn(query) match{
      case Some(_) => SparqlQueryType.SELECT
      case None => SparqlQueryType.ASK.regex.findFirstMatchIn(query) match {
        case Some(_) => SparqlQueryType.ASK
        case None => SparqlQueryType.CONSTRUCT.regex.findFirstMatchIn(query) match {
          case Some(_) => SparqlQueryType.CONSTRUCT
          case None => SparqlQueryType.DESCRIBE.regex.findFirstMatchIn(query) match {
            case Some(_) => SparqlQueryType.DESCRIBE
            case None => throw new IllegalArgumentException("The provided query does not match any known SPARQL query type!")
          }
        }
      }
    }
  }
}
