package org.silkframework.dataset.rdf

import org.silkframework.dataset.DataSourceCharacteristics.SupportedPathExpressions
import org.silkframework.dataset.{DataSourceCharacteristics, Dataset}

trait RdfDataset extends Dataset {

  def sparqlEndpoint: SparqlEndpoint

  /**
    * URI of the graph this RDF dataset is referring to, if any.
    */
  def graphOpt: Option[String] = None

  /** Shared characteristics of all RDF Datasets */
  override final val characteristics: DataSourceCharacteristics = {
    DataSourceCharacteristics(
      SupportedPathExpressions(
        multiHopPaths = true,
        backwardPaths = true,
        propertyFilter = true,
        languageFilter = true
      )
    )
  }

}
