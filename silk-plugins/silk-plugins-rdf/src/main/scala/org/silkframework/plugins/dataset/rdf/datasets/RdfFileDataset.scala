package org.silkframework.plugins.dataset.rdf.datasets

import org.apache.jena.query.DatasetFactory
import org.apache.jena.riot.{Lang, RDFDataMgr, RDFLanguages}
import org.silkframework.config.{PlainTask, Prefixes, Task}
import org.silkframework.dataset._
import org.silkframework.dataset.bulk.BulkResourceBasedDataset
import org.silkframework.dataset.rdf.{LinkFormatter, RdfDataset, SparqlParams}
import org.silkframework.entity.EntitySchema
import org.silkframework.entity.paths.TypedPath
import org.silkframework.entity.rdf.SparqlRestriction
import org.silkframework.execution.EntityHolder
import org.silkframework.execution.local.{EmptyEntityTable, GenericEntityTable}
import org.silkframework.plugins.dataset.rdf.access.SparqlSource
import org.silkframework.plugins.dataset.rdf.endpoint.{JenaEndpoint, JenaModelEndpoint}
import org.silkframework.plugins.dataset.rdf.formatters._
import org.silkframework.plugins.dataset.rdf.sparql.EntityRetriever
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.iterator.CloseableIterator
import org.silkframework.runtime.plugin.MultilineStringParameter
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}
import org.silkframework.runtime.resource.{Resource, WritableResource}
import org.silkframework.util.{Identifier, Uri}

@Plugin(
  id = "file",
  label = "RDF",
  categories = Array(DatasetCategories.file),
  description =
"""Dataset which retrieves and writes all entities from/to an RDF file.
The dataset is loaded in-memory and thus the size is restricted by the available memory.
Large datasets should be loaded into an external RDF store and retrieved using the SPARQL dataset instead.""")
case class RdfFileDataset(
  @Param("The RDF file. This may also be a zip archive of multiple RDF files.")
  file: WritableResource,
  @Param(
    value = """Optional RDF format. If left empty, it will be auto-detected based on the file extension. N-Triples is the only format that can be written, while other formats can only be read.""",
    autoCompletionProvider = classOf[RdfLangAutocompletionProvider]
  )
  format: String = "",
  @Param("The graph name to be read. If not provided, the default graph will be used. Must be provided if the format is N-Quads.")
  graph: String = "",
  @Param(value = "A list of entities to be retrieved. If not given, all entities will be retrieved. Multiple entities are separated by whitespace.", advanced = true)
  entityList: MultilineStringParameter = MultilineStringParameter(""),
  @Param(label = "ZIP file regex", value = "If the input resource is a ZIP file, files inside the file are filtered via this regex.", advanced = true)
  override val zipFileRegex: String = ".*") extends RdfDataset with TripleSinkDataset with BulkResourceBasedDataset {

  implicit val userContext: UserContext = UserContext.INTERNAL_USER

  /** The RDF format of the given resource. */
  private def lang = {
    // If the format is not specified explicitly, we try to guess it
    if (format.isEmpty) {
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
    if (lang == Lang.NTRIPLES) {
      NTriplesLinkFormatter()
    } else {
      throw new IllegalArgumentException(s"Unsupported output format. Currently only N-Triples is supported.")
    }
  }

  override def graphOpt: Option[String] = if (graph.trim.isEmpty) None else Some(graph)

  override def sparqlEndpoint: JenaEndpoint = {
    createSparqlEndpoint(retrieveResources())
  }

  private def createSparqlEndpoint(resources: CloseableIterator[Resource]): JenaEndpoint = {
    // Load data set
    val dataset = DatasetFactory.createTxnMem()
    resources.use { res =>
      for (resource <- res) {
        if (resource.exists) {
          val inputStream = resource.inputStream
          try {
            RDFDataMgr.read(dataset, inputStream, lang)
          } finally {
            inputStream.close()
          }
        }
      }
    }

    // Retrieve model
    val model =
      if (!graph.trim.isEmpty) {
        dataset.getNamedModel(graph)
      }
      else {
        dataset.getDefaultModel
      }

    new JenaModelEndpoint(model)
  }

  override def mergeSchemata: Boolean = true

  override def createSource(resource: Resource): DataSource = new FileSource(resource)

  override def linkSink(implicit userContext: UserContext): FormattedLinkSink = new FormattedLinkSink(file, formatter)

  override def entitySink(implicit userContext: UserContext): FormattedEntitySink = new FormattedEntitySink(file, formatter)

  // restrict the fetched entities to following URIs
  private def entityRestriction: Seq[Uri] = SparqlParams.splitEntityList(entityList.str).map(Uri(_))

  /** RDF file data source. */
  class FileSource(resource: Resource) extends DataSource with PeakDataSource with Serializable with SamplingDataSource
    with SchemaExtractionSource with SparqlRestrictionDataSource {

    // Load dataset
    private var endpoint: JenaEndpoint = _
    private var lastModificationTime: Option[(Long, Int)] = None

    override def retrieve(entitySchema: EntitySchema, limit: Option[Int] = None)
                         (implicit userContext: UserContext, prefixes: Prefixes): EntityHolder = {
      load()
      val retrievedEntities = EntityRetriever(endpoint).retrieve(entitySchema, entityRestriction, None)
      GenericEntityTable(retrievedEntities, entitySchema, underlyingTask)
    }

    override def retrieveByUri(entitySchema: EntitySchema, entities: Seq[Uri])
                              (implicit userContext: UserContext, prefixes: Prefixes): EntityHolder = {
      if (entities.isEmpty) {
        EmptyEntityTable(underlyingTask)
      } else {
        load()
        sparqlSource.retrieveByUri(entitySchema, entities)
      }
    }

    override def retrievePaths(typeUri: Uri, depth: Int, limit: Option[Int])
                              (implicit userContext: UserContext, prefixes: Prefixes): IndexedSeq[TypedPath] = {
      load()
      sparqlSource.retrievePaths(typeUri, depth, limit)
    }

    override def retrieveTypes(limit: Option[Int])
                              (implicit userContext: UserContext, prefixes: Prefixes): Traversable[(String, Double)] = {
      load()
      sparqlSource.retrieveTypes(limit)
    }

    /**
      * Loads the dataset and creates an endpoint.
      * Does nothing if the data set has already been loaded.
      */
    private def load(): Unit = synchronized {
      val modificationTime = file.modificationTime.map(mt => (mt.getEpochSecond, mt.getNano))
      if (endpoint == null || modificationTime != lastModificationTime) {
        file.checkSizeForInMemory()
        endpoint = createSparqlEndpoint(CloseableIterator(resource))
        lastModificationTime = modificationTime
      }
    }

    /**
      * The dataset task underlying the Datset this source belongs to
      */
    override lazy val underlyingTask: Task[DatasetSpec[Dataset]] = {
      PlainTask(Identifier.fromAllowed(file.name), DatasetSpec(EmptyDataset))
    } //FIXME CMEM 1352 replace with actual task

    override def retrievePathsSparqlRestriction(sparqlRestriction: SparqlRestriction,
                                                limit: Option[Int])
                                               (implicit userContext: UserContext): IndexedSeq[TypedPath] = {
      load()
      sparqlSource.retrievePathsSparqlRestriction(sparqlRestriction, limit)
    }

    override def sampleValues(typeUri: Option[Uri],
                              typedPaths: Seq[TypedPath],
                              valueSampleLimit: Option[Int])
                             (implicit userContext: UserContext): Seq[CloseableIterator[String]] = {
      load()
      sparqlSource.sampleValues(typeUri, typedPaths, valueSampleLimit)
    }

    override def extractSchema[T](analyzerFactory: ValueAnalyzerFactory[T],
                                  pathLimit: Int,
                                  sampleLimit: Option[Int],
                                  progressFN: Double => Unit)
                                 (implicit userContext: UserContext): ExtractedSchema[T] = {
      load()
      sparqlSource.extractSchema(analyzerFactory, pathLimit, sampleLimit, progressFN)
    }

    private def sparqlSource = new SparqlSource(SparqlParams(graph = graphOpt), endpoint)
  }

  override def tripleSink(implicit userContext: UserContext): TripleSink = new FormattedEntitySink(file, formatter)
}
