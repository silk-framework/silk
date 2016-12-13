package org.silkframework.dataset.rdf

import java.net.URI

/**
  * Datasets implementing this trait can be cleaned before writing to them.
  */
trait ClearableDatasetGraphTrait { this: RdfDataset =>
  /** Clears the graph*/
  def clearGraph(): Unit = {
    Option(graphToClear).filter(_.nonEmpty)
    // Test if the graph URI is a syntactically correct URI
    val uri = new URI(graphToClear)
    sparqlEndpoint.update(
      s"""
        |DROP SILENT GRAPH <${uri.toString}>
      """.stripMargin)
  }

  /**
    * The graph of the dataset that will be cleared when calling clearGraph.
    */
  def graphToClear: String

  /**
    * True if the dataset graph should be cleared before workflow execution.
    */
  def clearGraphBeforeExecution: Boolean
}
