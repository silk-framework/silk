package org.silkframework.plugins.dataset.rdf.endpoint

import java.io._
import java.util.logging.Logger

import org.apache.jena.query.{Dataset, Query, QueryExecution, QueryExecutionFactory}
import org.apache.jena.rdf.model.{Model, ModelFactory}
import org.apache.jena.riot.{Lang, RDFLanguages}
import org.apache.jena.update.{UpdateExecutionFactory, UpdateFactory, UpdateProcessor}
import org.silkframework.dataset.rdf.{GraphStoreTrait, SparqlEndpoint, SparqlParams}
import org.silkframework.runtime.activity.UserContext

/**
  * A SPARQL endpoint which executes all queries on a Jena Dataset.
  */
class JenaDatasetEndpoint(dataset: Dataset, val sparqlParams: SparqlParams = SparqlParams(pageSize = 0)) extends JenaEndpoint with GraphStoreTrait {

  override protected def createQueryExecution(query: Query): QueryExecution = {
    QueryExecutionFactory.create(query, dataset)
  }

  override def createUpdateExecution(query: String): UpdateProcessor = {
    UpdateExecutionFactory.create(UpdateFactory.create(query), dataset)
  }

  override def graphStoreEndpoint(graph: String): String = {
    graph
  }

  /**
    * Returns a copy of a specific graph of the underlying Dataset.
    * @param graphURI The URI of the graph to return.
    */
  def graphAsModelCopy(graphURI: String): Model = {
    val model = ModelFactory.createDefaultModel()
    model.add(dataset.getNamedModel(graphURI))
    model
  }

  override def postDataToGraph(graph: String,
                               contentType: String = "application/n-triples",
                               chunkedStreamingMode: Option[Int] = Some(1000),
                               comment: Option[String] = None)
                              (implicit userContext: UserContext): OutputStream = {
    val lang = Option(RDFLanguages.contentTypeToLang(contentType)).
        getOrElse(throw new IllegalArgumentException("Unknown content type: " + contentType))
    JenaDatasetWritingOutputStream(dataset, lang, graph)
  }

  override def getDataFromGraph(graph: String, acceptType: String)
                               (implicit userContext: UserContext): InputStream = {
    val strippedAccessType = acceptType.split(";").head
    val lang = Option(RDFLanguages.contentTypeToLang(strippedAccessType)).
        getOrElse(throw new IllegalArgumentException("Unknown accept type: " + acceptType))
    JenaDatasetWritingInputStream(dataset, lang, graph)
  }

  /**
    *
    * @param sparqlParams the new configuration of the SPARQL endpoint.
    * @return A SPARQL endpoint configured with the new parameters.
    */
  override def withSparqlParams(sparqlParams: SparqlParams): SparqlEndpoint = {
    new JenaDatasetEndpoint(dataset, sparqlParams)
  }

  override def graphStoreHeaders(userContext: UserContext): Map[String, String] = Map.empty
}

/**
  * Handles the sending of the request and the closing of the connection on closing the [[OutputStream]].
  */
case class JenaDatasetWritingOutputStream(dataset: Dataset, contentLang: Lang, graph: String) extends OutputStream {
  private val log: Logger = Logger.getLogger(this.getClass.getName)

  private lazy val outputStream = {
    new ByteArrayOutputStream()
  }

  override def write(i: Int): Unit = {
    outputStream.write(i)
  }

  override def close(): Unit = {
    val model = dataset.getNamedModel(graph)
    model.read(new ByteArrayInputStream(outputStream.toByteArray), null, contentLang.getName)
  }
}

case class JenaDatasetWritingInputStream(dataset: Dataset, contentLang: Lang, graph: String) extends InputStream {
  private lazy val inputStream = {
    val model = dataset.getNamedModel(graph)
    val out = new ByteArrayOutputStream()
    model.write(out, contentLang.getName)
    out.flush()
    val array = out.toByteArray
    new ByteArrayInputStream(array)
  }

  override def read(): Int = {
    inputStream.read()
  }

  override def read(b: Array[Byte]): Int = inputStream.read(b)

  override def close(): Unit = {
    inputStream.close()
  }
}