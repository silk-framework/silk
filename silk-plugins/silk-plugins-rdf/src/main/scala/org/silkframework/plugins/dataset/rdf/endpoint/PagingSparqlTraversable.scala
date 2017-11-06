package org.silkframework.plugins.dataset.rdf.endpoint

import java.io.IOException
import java.util.logging.{Level, Logger}

import org.apache.jena.query.{QueryFactory, Syntax}
import org.silkframework.dataset.rdf._

import scala.collection.immutable.SortedMap
import scala.xml.{Elem, Node, NodeSeq}

/**
  * Given a SPARQL query, pages through the results by issuing multiple queries with sliding offsets.
  */
object PagingSparqlTraversable {

  private val logger = Logger.getLogger(getClass.getName)

  /**
    * Given a SPARQL query, pages through the results by issuing multiple queries with sliding offsets.
    *
    * @param query The original query. If the query already contains an limit or offset automatic paging is disabled
    * @param queryExecutor A function that executes a SPARQL query and returns the XML result.
    * @param params The SPARQL parameters
    * @param limit The maximum number of SPARQL results returned in total (not per single query)
    */
  def apply(query: String, queryExecutor: String => Elem, params: SparqlParams, limit: Int): SparqlResults = {
    SparqlResults(
      bindings = new ResultsTraversable(query, queryExecutor, params, limit)
    )
  }

  private class ResultsTraversable(query: String,
                                   queryExecutor: String => Elem,
                                   params: SparqlParams, limit: Int) extends Traversable[SortedMap[String, RdfNode]] {

    private var blankNodeCount = 0

    private var lastQueryTime = 0L

    private val graphPatternRegex = """[Gg][Rr][Aa][Pp][Hh]\s+<""".r

    override def foreach[U](f: SortedMap[String, RdfNode] => U): Unit = {
      val parsedQuery = QueryFactory.create(query)
      // Don't set graph if the query is already containing a GRAPH pattern (not easily possible to check with parsed query)
      if(graphPatternRegex.findFirstIn(query).isEmpty) {
        params.graph foreach { graphURI =>
          parsedQuery.addGraphURI(graphURI)
        }
      }
      if (parsedQuery.hasLimit || parsedQuery.hasOffset) {
        val xml = executeQuery(parsedQuery.serialize(Syntax.syntaxSPARQL_11))
        val resultsXml = xml \ "results" \ "result"
        for (resultXml <- resultsXml) {
          f(parseResult(resultXml))
        }
      } else {
        for (offset <- 0 until limit by params.pageSize) {
          parsedQuery.setLimit(math.min(params.pageSize, limit - offset))
          parsedQuery.setOffset(offset)
          val xml = executeQuery(parsedQuery.serialize(Syntax.syntaxSPARQL_11))
          val resultsXml = xml \ "results" \ "result"
          for (resultXml <- resultsXml) {
            f(parseResult(resultXml))
          }
          if (resultsXml.size < params.pageSize) return
        }
      }
    }

    private def parseResult(resultXml: NodeSeq): SortedMap[String, RdfNode] = {
      val bindings = resultXml \ "binding"

      val uris = for (binding <- bindings; node <- binding \ "uri") yield ((binding \ "@name").text, Resource(node.text))

      val literals = for (binding <- bindings; node <- binding \ "literal") yield {
        parseLiteral(binding, node)
      }

      val bNodes = for (binding <- bindings; _ <- binding \ "bnode") yield {
        blankNodeCount += 1
        ((binding \ "@name").text, BlankNode("bnode" + blankNodeCount))
      }

      SortedMap(uris ++ literals ++ bNodes: _*)
    }

    /**
      * Executes a SPARQL SELECT query.
      *
      * @param query The SPARQL query to be executed
      * @return Query result in SPARQL Query Results XML Format
      */
    private def executeQuery(query: String): Elem = {
      //Wait until pause time is elapsed since last query
      synchronized {
        while (System.currentTimeMillis < lastQueryTime + params.pauseTime) Thread.sleep(params.pauseTime / 10)
        lastQueryTime = System.currentTimeMillis
      }

      //Execute query
      logger.fine("Executing query on \n" + query)

      var result: Elem = null
      var retries = 0
      val retryPause = params.retryPause
      while (result == null) {
        try {
          result = queryExecutor(query)
        }
        catch {
          case ex: IOException =>
            retries += 1
            if (retries > params.retryCount) {
              throw ex
            }
            logger.info(s"Query failed:\n$query\nError Message: '${ex.getMessage}'.\nRetrying in $retryPause ms. ($retries/${params.retryCount})")

            Thread.sleep(retryPause)
            //Double the retry pause up to a maximum of 1 hour
            //retryPause = math.min(retryPause * 2, 60 * 60 * 1000)
          case ex: Exception =>
            logger.log(Level.SEVERE, "Could not execute query:\n" + query, ex)
            throw ex
        }
      }

      //Return result
      result
    }
  }

  private def parseLiteral(binding: Node, node: Node) = {
    val attrMap = node.attributes.asAttrMap
    val value = node.text
    val bindingName = (binding \ "@name").text
    val literal = (attrMap.get("xml:lang"), attrMap.get("datatype")) match {
      case (Some(lang), _) =>
        LanguageLiteral(value, lang)
      case (_, Some(dataType)) =>
        DataTypeLiteral(value, dataType)
      case _ =>
        PlainLiteral(value)
    }
    (bindingName, literal)
  }
}
