package org.silkframework.plugins.dataset

import java.net.{URI, URISyntaxException}
import javax.inject.Inject

import org.silkframework.config.{Config, DefaultConfig}
import org.silkframework.dataset._
import org.silkframework.dataset.rdf.{RdfDataset, SparqlEndpoint}
import org.silkframework.runtime.plugin.{Param, Plugin, PluginRegistry}

import scala.collection.mutable
import scala.util.Try

@Plugin(
  id = "internal",
  label = "Internal",
  description =
      """Dataset for storing entities between workflow steps."""
)
case class InternalDataset(
  @Param(label = "graph URI", value = "The RDF graph that is used for storing internal data")
  graphUri: String = null) extends InternalDatasetTrait {

  protected lazy val internalDatasetPluginImpl = InternalDataset.byGraph(Option(graphUri))
}

trait InternalDatasetTrait extends Dataset with TripleSinkDataset with RdfDataset {
  protected def internalDatasetPluginImpl: Dataset
  private lazy val _internalDatasetPluginImpl = internalDatasetPluginImpl

  override def sparqlEndpoint: SparqlEndpoint = {
    _internalDatasetPluginImpl match {
      case rdfDataset: RdfDataset =>
        rdfDataset.sparqlEndpoint
      case _ =>
        throw new RuntimeException("Internal dataset implementation is no RdfDataset, cannot return SparqlEndpoint. ")
    }
  }

  override def tripleSink: TripleSink = {
    _internalDatasetPluginImpl match {
      case rdfDataset: TripleSinkDataset =>
        rdfDataset.tripleSink
      case _ =>
        throw new RuntimeException("Internal dataset cannot provide a triple sink!")
    }
  }

  override def source: DataSource = _internalDatasetPluginImpl.source

  override def linkSink: LinkSink = _internalDatasetPluginImpl.linkSink

  override def entitySink: EntitySink = _internalDatasetPluginImpl.entitySink

  override def clear(): Unit = _internalDatasetPluginImpl.clear()
}

/**
  * Holds the default internal endpoint.
  * At the moment, the default can only be set programmatically and not in the configuration.
  */
object InternalDataset {
  @Inject
  private var configMgr: Config = DefaultConfig.instance

  lazy val internalDatasetGraphPrefix = Try(configMgr().getString("dataset.internal.graphPrefix")).
      getOrElse("http://silkframework.org/internal/")

  private val byGraphDataset: mutable.Map[String, Dataset] = new mutable.HashMap[String, Dataset]()

  // The internal dataset for the default graph
  lazy val default: Dataset = createInternalDataset()

  def createInternalDataset(): Dataset = {
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
            createInternalDataset()
          )
        }
      case None =>
        default
    }
  }
}