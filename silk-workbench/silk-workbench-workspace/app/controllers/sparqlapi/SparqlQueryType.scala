package controllers.sparqlapi

import scala.util.matching.Regex

object SparqlQueryType extends Enumeration {
  case class QueryType(name: String, regex: Regex) extends super.Val(name)

  implicit def valueToVal(x: Value): Val = x.asInstanceOf[Val]

  val ASK         = QueryType("ASK", "(?s)(ask|ASK)\\s+(WHERE|where)\\s*\\{".r)
  val CONSTRUCT   = QueryType("CONSTRUCT", "(?s)(construct|CONSTRUCT)\\s*\\{\\.*(WHERE|where)\\s*\\{".r)
  val DESCRIBE    = QueryType("DESCRIBE", "(?s)(describe|DESCRIBE)\\s+".r)
  val SELECT      = QueryType("SELECT", "(?s)(select|SELECT)\\s+(\\?|\\*).*(WHERE|where)\\s*\\{".r)

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
