package org.silkframework.plugins.dataset.rdf

import org.apache.jena.query.DatasetFactory
import org.apache.jena.riot.{Lang, RDFDataMgr, RDFLanguages}
import org.silkframework.config.{PlainTask, Task}
import org.silkframework.dataset.rdf.{RdfDataset, SparqlParams}
import org.silkframework.dataset._
import org.silkframework.entity.rdf.SparqlRestriction
import org.silkframework.entity.{Entity, EntitySchema, Path, TypedPath}
import org.silkframework.plugins.dataset.rdf.endpoint.{JenaEndpoint, JenaModelEndpoint}
import org.silkframework.plugins.dataset.rdf.formatters._
import org.silkframework.plugins.dataset.rdf.sparql.{EntityRetriever, SparqlAggregatePathsCollector, SparqlTypesCollector}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.{MultilineStringParameter, Param, Plugin}
import org.silkframework.runtime.resource.WritableResource
import org.silkframework.util.{Identifier, Uri}

@Plugin(
  id = "file",
  label = "RDF file",
  description =
"""Dataset which retrieves and writes all entities from/to an RDF file.
The dataset is loaded in-memory and thus the size is restricted by the available memory.
Large datasets should be loaded into an external RDF store and retrieved using the Sparql dataset instead.""")
case class RdfFileDataset(
  @Param("File name inside the resources directory. In the Workbench, this is the '(projectDir)/resources' directory.")
  file: WritableResource,
  @Param("""Supported input formats are: "RDF/XML", "N-Triples", "N-Quads", "Turtle". Supported output formats are: "N-Triples".""")
  format: String,
  @Param("The graph name to be read. If not provided, the default graph will be used. Must be provided if the format is N-Quads.")
  graph: String = "",
  @Param(label = "Max. read size (MB)",
    value = "The maximum size of the RDF file resource for read operations. Since the whole dataset will be kept in-memory, this value should be kept low to guarantee stability.")
  maxReadSize: Long = 10,
  @Param("A list of entities to be retrieved. If not given, all entities will be retrieved. Multiple entities are separated by whitespace.")
  entityList: MultilineStringParameter = MultilineStringParameter("")) extends RdfDataset with TripleSinkDataset with ResourceBasedDataset {

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
    if(lang == Lang.NTRIPLES) {
      NTriplesLinkFormatter()
    } else {
      throw new IllegalArgumentException(s"Unsupported output format. Currently only N-Triples is supported.")
    }
  }

  private val graphOpt = if(graph.trim.isEmpty) None else Some(graph)

  override def sparqlEndpoint: JenaEndpoint = {
    // Load data set
    val dataset = DatasetFactory.createTxnMem()
    val inputStream = file.inputStream
    RDFDataMgr.read(dataset, inputStream, lang)
    inputStream.close()

    // Retrieve model
    val model =
      if (!graph.trim.isEmpty) { dataset.getNamedModel(graph) }
      else { dataset.getDefaultModel }

    new JenaModelEndpoint(model)
  }

  override def source(implicit userContext: UserContext): FileSource.type = FileSource

  override def linkSink(implicit userContext: UserContext): FormattedLinkSink = new FormattedLinkSink(file, formatter)

  override def entitySink(implicit userContext: UserContext): FormattedEntitySink = new FormattedEntitySink(file, formatter)

  // restrict the fetched entities to following URIs
  private def entityRestriction: Seq[Uri] = SparqlParams.splitEntityList(entityList.str).map(Uri(_))

  object FileSource extends DataSource with PeakDataSource with Serializable with SamplingDataSource with SchemaExtractionSource {

    // Load dataset
    private var endpoint: JenaEndpoint = null
    private var lastModificationTime: Option[(Long, Int)] = None

    override def retrieve(entitySchema: EntitySchema, limit: Option[Int] = None)
                         (implicit userContext: UserContext): Traversable[Entity] = {
      load()
      EntityRetriever(endpoint).retrieve(entitySchema, entityRestriction, None)
    }

    override def retrieveByUri(entitySchema: EntitySchema, entities: Seq[Uri])
                              (implicit userContext: UserContext): Traversable[Entity] = {
      if (entities.isEmpty) {
        Seq.empty
      } else {
        load()
        EntityRetriever(endpoint).retrieve(entitySchema, entities, None)
      }
    }

    override def retrievePaths(t: Uri, depth: Int, limit: Option[Int])
                              (implicit userContext: UserContext): IndexedSeq[Path] = {
      load()
      val restrictions = SparqlRestriction.forType(t)
      SparqlAggregatePathsCollector(endpoint, graphOpt, restrictions, limit)
    }

    override def retrieveTypes(limit: Option[Int])
                              (implicit userContext: UserContext): Traversable[(String, Double)] = {
      load()
      SparqlTypesCollector(endpoint, graphOpt, limit)
    }

    /**
      * Loads the dataset and creates an endpoint.
      * Does nothing if the data set has already been loaded.
      */
    private def load(): Unit = synchronized {
      val modificationTime = file.modificationTime.map(mt => (mt.getEpochSecond, mt.getNano))
      if (endpoint == null || modificationTime != lastModificationTime) {
        if (file.size.isEmpty) {
          throw new RuntimeException("File size could not be determined, ")
        } else if (file.size.get > maxReadSize * 1000 * 1000) {
          throw new RuntimeException(s"File size (${file.size.get / 1000000.0} MB) is larger than configured max. read size ($maxReadSize MB).")
        } else {
          endpoint = sparqlEndpoint
          lastModificationTime = modificationTime
        }
      }
    }

    /**
      * The dataset task underlying the Datset this source belongs to
      *
      * @return
      */
    override def underlyingTask: Task[DatasetSpec[Dataset]] = PlainTask(Identifier.fromAllowed(RdfFileDataset.this.file.name), DatasetSpec(EmptyDataset)) //FIXME CMEM 1352 replace with actual task

    override def sampleValues(typeUri: Option[Uri],
                              typedPaths: Seq[TypedPath],
                              valueSampleLimit: Option[Int])
                             (implicit userContext: UserContext): Seq[Traversable[String]] = {
      load()
      new SparqlSource(SparqlParams(), endpoint).sampleValues(typeUri, typedPaths, valueSampleLimit)
    }

    override def extractSchema[T](analyzerFactory: ValueAnalyzerFactory[T],
                                  pathLimit: Int,
                                  sampleLimit: Option[Int],
                                  progressFN: Double => Unit)
                                 (implicit userContext: UserContext): ExtractedSchema[T] = {
      load()
      new SparqlSource(SparqlParams(), endpoint).extractSchema(analyzerFactory, pathLimit, sampleLimit, progressFN)
    }
  }

  override def tripleSink(implicit userContext: UserContext): TripleSink = new FormattedEntitySink(file, formatter)
}
