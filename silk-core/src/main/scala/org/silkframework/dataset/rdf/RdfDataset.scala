package org.silkframework.dataset.rdf

import org.silkframework.dataset.DatasetCharacteristics.SupportedPathExpressions
import org.silkframework.dataset.{DatasetCharacteristics, Dataset}

trait RdfDataset extends Dataset {

  def sparqlEndpoint: SparqlEndpoint

  /**
    * URI of the graph this RDF dataset is referring to, if any.
    */
  def graphOpt: Option[String] = None

  /** Shared characteristics of all RDF Datasets */
  override final val characteristics: DatasetCharacteristics = {
    DatasetCharacteristics(
      SupportedPathExpressions(
        multiHopPaths = true,
        backwardPaths = true,
        propertyFilter = true,
        languageFilter = true
      )
    )
  }

}
