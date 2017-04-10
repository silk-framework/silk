package org.silkframework.plugins.dataset.rdf.endpoint

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, OutputStream, StringWriter}
import java.util.logging.Logger

import com.hp.hpl.jena.query.{Dataset, QueryExecution, QueryExecutionFactory}
import com.hp.hpl.jena.rdf.model.{Model, ModelFactory}
import com.hp.hpl.jena.update.{GraphStoreFactory, UpdateExecutionFactory, UpdateFactory, UpdateProcessor}
import org.apache.jena.riot.{Lang, RDFLanguages}
import org.silkframework.dataset.rdf.{GraphStoreTrait, SparqlEndpoint, SparqlParams}

/**
  * A SPARQL endpoint which executes all queries on a Jena Dataset.
  */
class JenaDatasetEndpoint(dataset: Dataset) extends JenaEndpoint with GraphStoreTrait {

  override protected def createQueryExecution(query: String): QueryExecution = {
    QueryExecutionFactory.create(query, dataset)
  }

  override def createUpdateExecution(query: String): UpdateProcessor = {
    val graphStore = GraphStoreFactory.create(dataset)
    UpdateExecutionFactory.create(UpdateFactory.create(query), graphStore)
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
                      chunkedStreamingMode: Option[Int] = Some(1000)): OutputStream = {
    val lang = Option(RDFLanguages.contentTypeToLang(contentType)).
        getOrElse(throw new IllegalArgumentException("Unknown content type: " + contentType))
    JenaDatasetWritingOutputStream(dataset, lang, graph)
  }

  /**
    * @return the SPARQL related configuration of this SPARQL endpoint.
    */
  override def sparqlParams: SparqlParams = SparqlParams(pageSize = 0)

  /**
    *
    * @param sparqlParams the new configuration of the SPARQL endpoint.
    * @return A SPARQL endpoint configured with the new parameters.
    */
  override def withSparqlParams(sparqlParams: SparqlParams): SparqlEndpoint = {
    this // SPARQL parameters have no effect on this type of endpoint
  }

  override def graphStoreHeaders(): Map[String, String] = Map.empty
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