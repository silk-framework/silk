package org.silkframework.plugins.dataset.rdf.datasets

import org.apache.jena.rdf.model.{Model, ModelFactory}
import org.silkframework.dataset.rdf.{RdfDataset, SparqlEndpoint, SparqlParams}
import org.silkframework.dataset.{DataSource, EntitySink, LinkSink}
import org.silkframework.plugins.dataset.rdf.endpoint.JenaModelEndpoint
import org.silkframework.plugins.dataset.rdf.access.{SparqlSink, SparqlSource}
import org.silkframework.runtime.activity.UserContext

case class JenaModelDataset() extends RdfDataset {

  private val sparqlParams = SparqlParams()

  @volatile
  var sparqlEndpoint: SparqlEndpoint = {
    new JenaModelEndpoint(ModelFactory.createDefaultModel())
  }

  /**
    * Returns a data source for reading entities from the data set.
    */
  override def source(implicit userContext: UserContext): DataSource = {
    new SparqlSource(sparqlParams, sparqlEndpoint)
  }

  /**
    * Returns a link sink for writing entity links to the data set.
    */
  override def linkSink(implicit userContext: UserContext): LinkSink = {
    new SparqlSink(sparqlParams, sparqlEndpoint, dropGraphOnClear = true)
  }

  /**
    * Returns an entity sink for writing entities to the data set.
    */
  override def entitySink(implicit userContext: UserContext): EntitySink = {
    new SparqlSink(sparqlParams, sparqlEndpoint, dropGraphOnClear = true)
  }
}

object JenaModelDataset {

  def apply(model: Model): JenaModelDataset = {
    val ds = JenaModelDataset()
    ds.sparqlEndpoint = new JenaModelEndpoint(model)
    ds
  }

}

