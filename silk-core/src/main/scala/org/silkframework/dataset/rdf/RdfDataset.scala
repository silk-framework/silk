package org.silkframework.dataset.rdf

import org.silkframework.dataset.Dataset

trait RdfDataset extends Dataset {

  def sparqlEndpoint: SparqlEndpoint

  /**
    * URI of the graph this RDF dataset is referring to, if any.
    */
  def graphOpt: Option[String] = None

}
