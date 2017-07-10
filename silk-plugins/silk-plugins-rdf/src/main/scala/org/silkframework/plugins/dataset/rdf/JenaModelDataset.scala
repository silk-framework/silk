package org.silkframework.plugins.dataset.rdf

import com.hp.hpl.jena.rdf.model.Model
import org.silkframework.dataset.{DataSource, Dataset, EntitySink, LinkSink}
import org.silkframework.dataset.rdf.{RdfDataset, SparqlEndpoint, SparqlParams}
import org.silkframework.plugins.dataset.rdf.endpoint.JenaModelEndpoint

case class JenaModelDataset(model: Model) extends RdfDataset {

  private val sparqlParams = SparqlParams()

  override val sparqlEndpoint: SparqlEndpoint = {
    new JenaModelEndpoint(model)
  }

  /**
    * Returns a data source for reading entities from the data set.
    */
  override def source: DataSource = {
    new SparqlSource(sparqlParams, sparqlEndpoint)
  }

  /**
    * Returns a link sink for writing entity links to the data set.
    */
  override def linkSink: LinkSink = {
    new SparqlSink(sparqlParams, sparqlEndpoint)
  }

  /**
    * Returns a entity sink for writing entities to the data set.
    */
  override def entitySink: EntitySink = {
    new SparqlSink(sparqlParams, sparqlEndpoint)
  }
}
