package de.fuberlin.wiwiss.silk.plugins.dataset.rdf

import com.hp.hpl.jena.query.DatasetFactory
import de.fuberlin.wiwiss.silk.dataset.rdf.RdfDatasetPlugin
import de.fuberlin.wiwiss.silk.dataset.DataSource
import de.fuberlin.wiwiss.silk.entity.rdf.{SparqlRestriction, SparqlEntitySchema}
import de.fuberlin.wiwiss.silk.entity.Path
import de.fuberlin.wiwiss.silk.plugins.dataset.rdf.endpoint.{JenaEndpoint, JenaModelEndpoint}
import de.fuberlin.wiwiss.silk.plugins.dataset.rdf.formatters.{Formatter, NTriplesFormatter, FormattedDataSink}
import de.fuberlin.wiwiss.silk.plugins.dataset.rdf.sparql.{EntityRetriever, SparqlAggregatePathsCollector, SparqlTypesCollector}
import de.fuberlin.wiwiss.silk.runtime.plugin.Plugin
import de.fuberlin.wiwiss.silk.runtime.resource.Resource
import de.fuberlin.wiwiss.silk.util.Uri
import org.apache.jena.riot.{Lang, RDFDataMgr, RDFLanguages}

@Plugin(
  id = "file",
  label = "RDF dump",
  description =
    """ Dataset which retrieves and writes all entities from/to an RDF file.
      | Parameters:
      |  file: File name inside the resources directory. In the Workbench, this is the '(projectDir)/resources' directory.
      |  format: Supported formats are: "RDF/XML", "N-Triples", "N-Quads", "Turtle"
      |  graph: The graph name to be read. If not provided, the default graph will be used. Must be provided if the format is N-Quads.
    """
)
case class FileDataset(file: Resource, format: String, graph: String = "") extends RdfDatasetPlugin {

  /** The RDF format of the given resource. */
  private val lang = {
    // If the format is not specified explicitly, we try to guess it
    if(format.isEmpty) {
      val guessedLang = RDFLanguages.filenameToLang(file.name)
      require(guessedLang != null, "Cannot guess RDF format from resource name. Please specify it explicitly using the 'format' parameter.")
      guessedLang
    } else {
      val explicitLang = RDFLanguages.nameToLang(format)
      require(explicitLang != null, "Invalid format. Supported formats are: \"RDF/XML\", \"N-Triples\", \"N-Quads\", \"Turtle\"")
      explicitLang
    }
  }

  /** Currently RDF is written using custom formatters (instead of using an RDF writer from Jena). */
  private def formatter: Formatter = {
    if(lang == Lang.NTRIPLES)
      NTriplesFormatter()
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

  override def sink = new FormattedDataSink(file, formatter)

  object FileSource extends DataSource {

    // Load dataset
    private var endpoint: JenaEndpoint = null

    override def retrieveSparqlEntities(entityDesc: SparqlEntitySchema, entities: Seq[String]) = {
      load()
      EntityRetriever(endpoint).retrieve(entityDesc, entities.map(Uri(_)), None)
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
