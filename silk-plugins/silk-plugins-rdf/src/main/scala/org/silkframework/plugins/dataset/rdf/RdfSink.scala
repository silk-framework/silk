package org.silkframework.plugins.dataset.rdf

import java.io.ByteArrayOutputStream

import org.apache.jena.riot.{Lang, RDFDataMgr}
import org.silkframework.entity.{Link, ValueType}
import org.silkframework.plugins.dataset.rdf.formatters.RdfFormatter
import org.silkframework.runtime.activity.UserContext

/**
  *
  */
trait RdfSink {

  def writeStatement(subject: String, property: String, value: String, valueType: ValueType)
                    (implicit userContext: UserContext): Unit

  def formatterOpt: Option[RdfFormatter]

  /**
    * Returns the RDF formatted link in N-Triples format and the number of triples.
    *
    * @param link
    * @param predicateUri
    * @return (serialized statements as N-Triples, triple count)
    */
  def formatLink(link: Link,
                           predicateUri: String): (String, Int) = {
    formatterOpt match {
      case Some(formatter) =>
        val model = formatter.formatAsRDF(link, predicateUri)
        val outputStream = new ByteArrayOutputStream()
        RDFDataMgr.write(outputStream, model, Lang.NTRIPLES)
        val result = outputStream.toString("UTF-8")
        (result, result.split("\n").length)
      case None =>
        val result = "<" + link.source + "> <" + predicateUri + "> <" + link.target + "> .\n"
        (result, 1)
    }
  }

  def buildStatementString(subject: String, property: String, value: String, valueType: ValueType): String = {
    RdfFormatUtil.tripleValuesToNTriplesSyntax(subject, property, value, valueType)
  }
}
