package org.silkframework.plugins.dataset.rdf.endpoint

import com.hp.hpl.jena.query.DatasetFactory
import org.apache.jena.riot.{RDFDataMgr, RDFLanguages}
import org.silkframework.dataset.rdf.{SparqlParams, SparqlEndpoint, SparqlResults}
import org.silkframework.runtime.resource.Resource

class FileSparqlEndpoint(resource: Resource, graph: Option[String] = None, format: Option[String] = None) extends SparqlEndpoint {

  /** The RDF format of the given resource. */
  private val lang = {
    // If the format is not specified explicitly, we try to guess it
    format match {
      case Some(f) =>
        val explicitLang = RDFLanguages.nameToLang(f)
        require(explicitLang != null, "Invalid format. Supported formats are: \"RDF/XML\", \"N-Triples\", \"N-Quads\", \"Turtle\"")
        explicitLang
      case None =>
        val guessedLang = RDFLanguages.filenameToLang(resource.name)
        require(guessedLang != null, "Cannot guess RDF format from resource name. Please specify it explicitly using the 'format' parameter.")
        guessedLang
    }
  }

  // Loaded Jena model
  private val model = {
    val dataset = DatasetFactory.createMem()
    val inputStream = resource.load
    RDFDataMgr.read(dataset, inputStream, lang)
    inputStream.close()

    graph match {
      case Some(g) => dataset.getNamedModel(g)
      case None =>   dataset.getDefaultModel
    }
  }

  private val jenaEndpoint = new JenaModelEndpoint(model)

  /**
    * Executes a select query.
    * If the query does not contain a offset or limit, automatic paging is done by issuing multiple queries with a sliding offset.
    *
    */
  override def select(query: String, limit: Int): SparqlResults = jenaEndpoint.select(query, limit)

  /**
    * Executes a construct query.
    */
  override def construct(query: String): String = jenaEndpoint.construct(query)

  /**
    * Executes an update query.
    */
  override def update(query: String): Unit = jenaEndpoint.update(query)

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
    this // No SPARQL parameters supported
  }
}

