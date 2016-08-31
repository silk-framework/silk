package org.silkframework.plugins.dataset

import java.net.{URI, URISyntaxException}

import org.silkframework.config.Config
import org.silkframework.dataset.rdf.{SparqlEndpoint, RdfDataset}
import org.silkframework.dataset._
import org.silkframework.runtime.plugin.{Plugin, PluginRegistry}

import scala.collection.mutable
import scala.util.Try

@Plugin(
  id = "internal",
  label = "Internal",
  description =
      """Dataset for storing entities between workflow steps."""
)
case class InternalDataset(val graphUri: String = null) extends Dataset with TripleSinkDataset with RdfDataset {
  private val internalDatasetPluginImpl = InternalDataset.byGraph(Option(graphUri))

  override def source: DataSource = internalDatasetPluginImpl.source

  override def linkSink: LinkSink = internalDatasetPluginImpl.linkSink

  override def entitySink: EntitySink = internalDatasetPluginImpl.entitySink

  override def clear() = internalDatasetPluginImpl.clear()

  override def sparqlEndpoint: SparqlEndpoint = {
    internalDatasetPluginImpl match {
      case rdfDataset: RdfDataset =>
        rdfDataset.sparqlEndpoint
      case _ =>
        throw new RuntimeException("Internal dataset is not ")
    }
  }

  override def tripleSink: TripleSink = {
    internalDatasetPluginImpl match {
      case rdfDataset: TripleSinkDataset =>
        rdfDataset.tripleSink
      case _ =>
        throw new RuntimeException("Internal dataset cannot provide a triple sink!")
    }
  }
}

/**
  * Holds the default internal endpoint.
  * At the moment, the default can only be set programmatically and not in the configuration.
  */
object InternalDataset {
  val internalDatasetGraphPrefix = Try(Config().getString("dataset.internal.graphPrefix")).
      getOrElse("http://silkframework.org/internal/")

  private val byGraphDataset: mutable.Map[String, Dataset] = new mutable.HashMap[String, Dataset]()

  // The internal dataset for the default graph
  lazy val default: Dataset = createInternalDataset

  private def createInternalDataset: Dataset = {
    // TODO: For non-in-memory datasets the graph must be handed over
    PluginRegistry.createFromConfigOption[Dataset]("dataset.internal") match {
      case Some(dataset) =>
        dataset
      case None =>
        throw new IllegalAccessException("No internal dataset plugin has been configured at 'dataset.internal'.")
    }
  }

  /**
    * Returns the internal dataset for a specific graph
    *
    * @param graphUriOpt A graph or None for the default graph
    * @return
    */
  def byGraph(graphUriOpt: Option[String]): Dataset = {
    graphUriOpt match {
      case Some(graphURI) =>
        try {
          new URI(graphURI)
        } catch {
          case e: URISyntaxException =>
            throw new RuntimeException("Not a valid URI: " + graphURI)
        }
        byGraphDataset.synchronized {
          byGraphDataset.getOrElseUpdate(
            graphURI,
            createInternalDataset
          )
        }
      case None =>
        default
    }
  }
}