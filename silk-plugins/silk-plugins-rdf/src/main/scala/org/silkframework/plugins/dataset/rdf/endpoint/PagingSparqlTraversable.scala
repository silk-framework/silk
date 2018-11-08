package org.silkframework.plugins.dataset.rdf.endpoint

import java.io.{IOException, InputStream}
import java.util.logging.{Level, Logger}

import javax.xml.stream.{XMLInputFactory, XMLStreamConstants, XMLStreamReader}
import org.apache.jena.query.{QueryFactory, Syntax}
import org.silkframework.dataset.rdf._

import scala.collection.immutable.SortedMap
import scala.collection.mutable.ArrayBuffer
import scala.util.Try
import scala.util.matching.Regex
import scala.xml.XML

/**
  * Given a SPARQL query, pages through the results by issuing multiple queries with sliding offsets.
  */
object PagingSparqlTraversable {

  val graphPatternRegex: Regex = """[Gg][Rr][Aa][Pp][Hh]\s+<""".r
  private val xmlFactory = XMLInputFactory.newInstance()

  private val logger = Logger.getLogger(getClass.getName)

  /**
    * Given a SPARQL query, pages through the results by issuing multiple queries with sliding offsets.
    *
    * @param query The original query. If the query already contains an limit or offset automatic paging is disabled
    * @param queryExecutor A function that executes a SPARQL query and returns a Traversable over the SPARQL results.
    * @param params The SPARQL parameters
    * @param limit The maximum number of SPARQL results returned in total (not per single query)
    */
  def apply(query: String, queryExecutor: String => InputStream, params: SparqlParams, limit: Int): SparqlResults = {
    SparqlResults(
      bindings = new ResultsTraversable(query, queryExecutor, params, limit)
    )
  }

  private class ResultsTraversable(query: String,
                                   queryExecutor: String => InputStream,
                                   params: SparqlParams,
                                   limit: Int) extends Traversable[SortedMap[String, RdfNode]] {

    private var blankNodeCount = 0

    private var lastQueryTime = 0L

    private val RESULT_TAG = "result"

    override def foreach[U](f: SortedMap[String, RdfNode] => U): Unit = {
      val parsedQuery = QueryFactory.create(query)
      // Don't set graph if the query is already containing a GRAPH pattern (not easily possible to check with parsed query)
      if(graphPatternRegex.findFirstIn(query).isEmpty) {
        params.graph foreach { graphURI =>
          parsedQuery.addGraphURI(graphURI)
        }
      }

      if (parsedQuery.hasLimit || parsedQuery.hasOffset) {
        val (xmlReader, inputStream) = executeQuery(parsedQuery.serialize(Syntax.syntaxSPARQL_11))
        try {
          outputResults(xmlReader, f)
        } finally {
          inputStream.close()
        }
      } else {
        for (offset <- 0 until limit by params.pageSize) {
          parsedQuery.setLimit(math.min(params.pageSize, limit - offset))
          parsedQuery.setOffset(offset)
          val (xmlReader, inputStream) = executeQuery(parsedQuery.serialize(Syntax.syntaxSPARQL_11))
          try {
            val resultCount = outputResults(xmlReader, f)
            logger.fine("Run: " + offset + ", " + limit + ", " + params.pageSize)
            if (resultCount < params.pageSize) return
          } finally {
            inputStream.close()
          }
        }
      }
    }

    private def isResult(xmlReader: XMLStreamReader) = {
      xmlReader.isStartElement && xmlReader.getLocalName == RESULT_TAG
    }

    def outputResults[U](xmlReader: XMLStreamReader,
                         f: SortedMap[String, RdfNode] => U): Int = {
      placeOnTag(xmlReader, RESULT_TAG)
      var resultCount = 0
      while (isResult(xmlReader)) {
        resultCount += 1
        f(parseResult(xmlReader))
        placeOnTag(xmlReader, RESULT_TAG)
      }
      xmlReader.close()
      resultCount
    }

    // Place stream reader on next start or end tag
    def placeOnNextTag(streamReader: XMLStreamReader): Unit = {
      if(streamReader.hasNext) {
        streamReader.next()
        while (streamReader.getEventType != XMLStreamConstants.START_ELEMENT && streamReader.getEventType != XMLStreamConstants.END_ELEMENT && streamReader.hasNext) {
          streamReader.next()
        }
      }
    }

    def placeOnTag(streamReader: XMLStreamReader, tag: String, eventType: Int = XMLStreamConstants.START_ELEMENT): Unit = {
      while(streamReader.hasNext && !(streamReader.getEventType == eventType && streamReader.getLocalName == tag)) {
        streamReader.next()
      }
    }

    private def parseResult(xmlReader: XMLStreamReader): SortedMap[String, RdfNode] = {
      val resultBuffer = ArrayBuffer[(String, RdfNode)]()
      placeOnNextTag(xmlReader)
      while(xmlReader.isStartElement && xmlReader.getLocalName == "binding") {
        val name = xmlReader.getAttributeValue(null, "name")
        placeOnNextTag(xmlReader) // Place on e.g. <literal>
        val rdfNode = parseRdfNode(xmlReader)
        resultBuffer.append((name, rdfNode))
        placeOnTag(xmlReader, "binding", eventType = XMLStreamConstants.END_ELEMENT)
        placeOnNextTag(xmlReader) // This might be <binding> or </result>
      }
      SortedMap(resultBuffer: _*)
    }

    private def parseRdfNode(xmlReader: XMLStreamReader): RdfNode = {
      def text = {
        xmlReader.next()
        if(xmlReader.getEventType == XMLStreamConstants.END_ELEMENT) {
          ""
        } else {
          assert(xmlReader.getEventType == XMLStreamConstants.CHARACTERS, "Expected characters, but found type " + xmlReader.getEventType)
          xmlReader.getText
        }
      }
      xmlReader.getLocalName match {
        case "bnode" =>
          BlankNode(text)
        case "uri" =>
          Resource(text)
        case "literal" =>
          val lang = Option(xmlReader.getAttributeValue(XML.namespace, "lang"))
          val dataType = Option(xmlReader.getAttributeValue(null, "datatype"))
          (lang, dataType) match {
            case (Some(l), _) =>
              LanguageLiteral(text, l)
            case (_, Some(dt)) =>
              DataTypeLiteral(text, dt)
            case _ =>
              PlainLiteral(text)
           }
      }
    }

    /**
      * Executes a SPARQL SELECT query.
      *
      * @param query The SPARQL query to be executed
      * @return an XML stream reader positioned on the <results> tag.
      */
    private def executeQuery(query: String): (XMLStreamReader, InputStream) = {
      //Wait until pause time is elapsed since last query
      synchronized {
        while (System.currentTimeMillis < lastQueryTime + params.pauseTime) Thread.sleep(params.pauseTime / 10)
        lastQueryTime = System.currentTimeMillis
      }
      //Execute query
      logger.fine("Executing query on \n" + query)

      var xmlStreamReader: XMLStreamReader = null
      var inputStream: InputStream = null
      var retries = 0
      val retryPause = params.retryPause

      def closeInputStream() = {
        if (inputStream != null) {
          Try(inputStream.close())
        }
      }

      while (xmlStreamReader == null) {
        try {
          val is = queryExecutor(query)
          inputStream = is
          val reader = xmlFactory.createXMLStreamReader(is)
          while(reader.hasNext && !(reader.isStartElement && reader.getLocalName == "results")) {
            placeOnNextTag(reader)
          }
          xmlStreamReader = reader
        } catch {
          case ex: IOException =>
            retries += 1
            if (retries > params.retryCount) {
              throw ex
            }
            logger.info(s"Query failed:\n$query\nError Message: '${ex.getMessage}'.\nRetrying in $retryPause ms. ($retries/${params.retryCount})")

            Thread.sleep(retryPause)
            closeInputStream()
            //Double the retry pause up to a maximum of 1 hour
            //retryPause = math.min(retryPause * 2, 60 * 60 * 1000)
          case ex: Exception =>
            logger.log(Level.SEVERE, "Could not execute query:\n" + query, ex)
            closeInputStream()
            throw ex
        }
      }
      //Return result
      (xmlStreamReader, inputStream)
    }
  }
}
