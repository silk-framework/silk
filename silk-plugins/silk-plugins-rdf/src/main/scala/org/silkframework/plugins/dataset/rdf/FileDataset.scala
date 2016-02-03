package org.silkframework.plugins.dataset.rdf

import com.hp.hpl.jena.query.DatasetFactory
import org.silkframework.dataset.rdf.RdfDatasetPlugin
import org.silkframework.dataset.DataSource
import org.silkframework.entity.rdf.{SparqlRestriction, SparqlEntitySchema}
import org.silkframework.entity.{Entity, EntitySchema, Path}
import org.silkframework.plugins.dataset.rdf.endpoint.{JenaEndpoint, JenaModelEndpoint}
import org.silkframework.plugins.dataset.rdf.formatters._
import org.silkframework.plugins.dataset.rdf.sparql.{EntityRetriever, SparqlAggregatePathsCollector, SparqlTypesCollector}
import org.silkframework.runtime.plugin.{Param, Plugin}
import org.silkframework.runtime.resource.{WritableResource, Resource}
import org.silkframework.util.Uri
import org.apache.jena.riot.{Lang, RDFDataMgr, RDFLanguages}

@Plugin(
  id = "file",
  label = "RDF dump",
  description = "Dataset which retrieves and writes all entities from/to an RDF file.")
case class FileDataset(
  @Param("File name inside the resources directory. In the Workbench, this is the '(projectDir)/resources' directory.")
  file: WritableResource,
  @Param("""Supported formats are: "RDF/XML", "N-Triples", "N-Quads", "Turtle".""")
  format: String,
  @Param("The graph name to be read. If not provided, the default graph will be used. Must be provided if the format is N-Quads.")
  graph: String = "") extends RdfDatasetPlugin {

  /** The RDF format of the given resource. */
  private val lang = {
    // If the format is not specified explicitly, we try to guess it
    if(format.isEmpty) {
      val guessedLang = RDFLanguages.filenameToLang(file.name)
      require(guessedLang != null, "Cannot guess RDF format from resource name. Please specify it explicitly using the 'format' parameter.")
      guessedLang
    } else {
      val explicitLang = RDFLanguages.nameToLang(format)
      require(explicitLang != null, s"Invalid format '$format'. Supported formats are: 'RDF/XML', 'N-Triples', 'N-Quads', 'Turtle'")
      explicitLang
    }
  }

  /** Currently RDF is written using custom formatters (instead of using an RDF writer from Jena). */
  private def formatter: LinkFormatter with EntityFormatter = {
    if(lang == Lang.NTRIPLES)
      NTriplesLinkFormatter()
    else
      throw new IllegalArgumentException(s"Unsupported output format. Currently only N-Triples is supported.")
  }

  override def sparqlEndpoint = {
    // Load data set
    val dataset = DatasetFactory.createMem()
    val inputStream = file.load
    RDFDataMgr.read(dataset, inputStream, lang)
    inputStream.close()

    // Retrieve model
    val model =
      if (!graph.trim.isEmpty) dataset.getNamedModel(graph)
      else dataset.getDefaultModel

    new JenaModelEndpoint(model)
  }

  override def source = FileSource

  override def linkSink = new FormattedLinkSink(file, formatter)

  override def entitySink = new FormattedEntitySink(file, formatter)

  override def clear(): Unit = { }

  object FileSource extends DataSource {

    // Load dataset
    private var endpoint: JenaEndpoint = null

    override def retrieve(entitySchema: EntitySchema, limit: Option[Int] = None): Traversable[Entity] = {
      load()
      EntityRetriever(endpoint).retrieve(entitySchema, Seq.empty, None)
    }

    override def retrieveByUri(entitySchema: EntitySchema, entities: Seq[Uri]): Seq[Entity] = {
      load()
      EntityRetriever(endpoint).retrieve(entitySchema, entities, None).toSeq
    }

    override def retrieveSparqlPaths(restrictions: SparqlRestriction, depth: Int, limit: Option[Int]): Traversable[(Path, Double)] = {
      load()
      SparqlAggregatePathsCollector(endpoint, restrictions, limit).map(p => (p, 1.0))
    }

    override def retrieveTypes(limit: Option[Int]): Traversable[(String, Double)] = {
      load()
      SparqlTypesCollector(endpoint, limit)
    }

    /**
     * Loads the dataset and creates an endpoint.
     * Does nothing if the data set has already been loaded.
     */
    private def load() = synchronized {
      if (endpoint == null) {
        endpoint = sparqlEndpoint
      }
    }
  }
}
