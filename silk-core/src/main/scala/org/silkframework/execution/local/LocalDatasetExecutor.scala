package org.silkframework.execution.local

import org.silkframework.config.{Prefixes, Task, TaskSpec}
import org.silkframework.dataset.CloseableDataset.using
import org.silkframework.dataset.DatasetSpec.{EntitySinkWrapper, GenericDatasetSpec}
import org.silkframework.dataset._
import org.silkframework.dataset.rdf._
import org.silkframework.entity._
import org.silkframework.execution._
import org.silkframework.execution.report.EntitySample
import org.silkframework.runtime.activity.{ActivityContext, UserContext}
import org.silkframework.runtime.iterator.{CloseableIterator, TraversableIterator}
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.Uri

import java.util
import java.util.logging.{Level, Logger}

/**
  * Local dataset executor that handles read and writes to [[Dataset]] tasks.
  */
abstract class LocalDatasetExecutor[DatasetType <: Dataset] extends DatasetExecutor[DatasetType, LocalExecution] {
  private val logger = Logger.getLogger(getClass.getName)

  /**
    * Reads data from a dataset.
    */
  override def read(dataset: Task[DatasetSpec[DatasetType]], schema: EntitySchema, execution: LocalExecution)
                   (implicit userContext: UserContext, context: ActivityContext[ExecutionReport], prefixes: Prefixes): LocalEntities = {
    //FIXME CMEM-1759 clean this and use only plugin based implementations of LocalEntities
    lazy val source = access(dataset, execution).source
    schema match {
      case EmptyEntityTable.schema =>
        EmptyEntityTable(dataset)
      case QuadEntityTable.schema =>
        handleTripleEntitySchema(dataset)
      case TripleEntityTable.schema =>
        handleTripleEntitySchema(dataset)
      case SparqlEndpointEntitySchema.schema =>
        handleSparqlEndpointSchema(dataset)
      case multi: MultiEntitySchema =>
        handleMultiEntitySchema(dataset, source, schema, multi)
      case DatasetResourceEntitySchema.schema =>
        handleDatasetResourceEntitySchema(dataset)
      case _ =>
        implicit val executionReport: ExecutionReportUpdater = ReadEntitiesReportUpdater(dataset, context)
        val table = source.retrieve(entitySchema = schema)
        GenericEntityTable(ReportingIterator(table.entities), entitySchema = schema, dataset, table.globalErrors)
    }
  }

  private def handleDatasetResourceEntitySchema(dataset: Task[DatasetSpec[DatasetType]]) = {
    dataset.data match {
      case datasetSpec: DatasetSpec[_] =>
        datasetSpec.plugin match {
          case dsr: ResourceBasedDataset =>
            new LocalDatasetResourceEntityTable(dsr.file, dataset)
          case _: Dataset =>
            throw new ValidationException(s"Dataset task ${dataset.id} of type " +
                s"${datasetSpec.plugin.pluginSpec.label} has no resource (file) or does not support requests for its resource!")
        }
      case _ =>
        throw new ValidationException("No dataset spec found!")
    }
  }

  private def handleMultiEntitySchema(dataset: Task[DatasetSpec[Dataset]], source: DataSource, schema: EntitySchema, multi: MultiEntitySchema)
                                     (implicit userContext: UserContext, prefixes: Prefixes, context: ActivityContext[ExecutionReport])= {
    implicit val executionReport: ExecutionReportUpdater = ReadEntitiesReportUpdater(dataset, context)
    val table = source.retrieve(entitySchema = schema)
    MultiEntityTable(
      entities = ReportingIterator(table.entities),
      entitySchema = schema,
      subTables =
          for (subSchema <- multi.subSchemata) yield
            GenericEntityTable(ReportingIterator(source.retrieve(entitySchema = subSchema).entities), subSchema, dataset),
      task = dataset,
      globalErrors = table.globalErrors
    )
  }

  private def handleTripleEntitySchema(dataset: Task[DatasetSpec[Dataset]])
                                      (implicit userContext: UserContext): TripleEntityTable = {
    dataset.data match {
      case rdfDataset: RdfDataset =>
        readTriples(dataset, rdfDataset)
      case DatasetSpec(rdfDataset: RdfDataset, _, _) =>
        readTriples(dataset, rdfDataset)
      case _ =>
        throw TaskException("Dataset is not a RDF dataset and thus cannot output triples!")
    }
  }
  private def handleSparqlEndpointSchema(dataset: Task[GenericDatasetSpec]): SparqlEndpointEntityTable = {
    dataset.data match {
      case rdfDataset: RdfDataset =>
        new SparqlEndpointEntityTable(rdfDataset.sparqlEndpoint, dataset)
      case DatasetSpec(rdfDataset: RdfDataset, _, _) =>
        new SparqlEndpointEntityTable(rdfDataset.sparqlEndpoint, dataset)
      case _ =>
        throw TaskException("Dataset does not offer a SPARQL endpoint!")
    }
  }

  private def readTriples(dataset: Task[GenericDatasetSpec], rdfDataset: RdfDataset)
                         (implicit userContext: UserContext): TripleEntityTable = {
    val sparqlResult = rdfDataset.sparqlEndpoint.select("SELECT ?s ?p ?o WHERE {?s ?p ?o}")
    val tripleEntities = sparqlResult.bindings map { resultMap =>
      val s = resultMap("s").value
      val p = resultMap("p").value
      val (value, typ) = TripleEntityTable.convertToEncodedType(resultMap("o"))
      Entity(s, IndexedSeq(Seq(s), Seq(p), Seq(value), Seq(typ)), TripleEntityTable.schema)
    }
    new TripleEntityTable(tripleEntities, dataset)
  }

  override protected def write(data: LocalEntities, dataset: Task[DatasetSpec[DatasetType]], execution: LocalExecution)
                              (implicit userContext: UserContext, context: ActivityContext[ExecutionReport], prefixes: Prefixes): Unit = {
    DatasetSpec.checkDatasetAllowsWriteAccess(Some(dataset.fullLabel), dataset.readOnly)
    //FIXME CMEM-1759 clean this and use only plugin based implementations of LocalEntities
    data match {
      case LinksTable(links, linkType, inverseLinkType, _) =>
        withLinkSink(dataset, execution) { linkSink =>
          writeLinks(dataset, linkSink, links, linkType, inverseLinkType)
        }
      case tripleEntityTable: TripleEntityTable =>
        withEntitySink(dataset, execution) { entitySink =>
          writeTriples(entitySink, tripleEntityTable.entities)
        }
      case QuadEntityTable(entitiesFunc, _) =>
        withEntitySink(dataset, execution) { entitySink =>
          writeTriples(entitySink, entitiesFunc())
        }
      case tables: MultiEntityTable =>
        implicit val report: ExecutionReportUpdater = WriteEntitiesReportUpdater(dataset, context)
        if(tables.subTables.nonEmpty && !dataset.data.characteristics.supportsMultipleTables) {
          throw new ValidationException(s"Dataset '${dataset.fullLabel}' does not support multiple tables, but '${data.task.fullLabel}'" +
            " provided multiple tables (e.g., because it's a hierarchical transformation).")
        }
        withEntitySink(dataset, execution) { entitySink =>
          writeMultiTables(entitySink, tables)
        }
        report.executionDone()
      case datasetResource: DatasetResourceEntityTable =>
        writeDatasetResource(dataset, datasetResource)
      case graphStoreFiles: LocalGraphStoreFileUploadTable =>
        val reportUpdater = UploadFilesViaGspReportUpdater(dataset, context)
        uploadFilesViaGraphStore(dataset, graphStoreFiles, reportUpdater)
      case sparqlUpdateTable: SparqlUpdateEntityTable =>
        executeSparqlUpdateQueries(dataset, sparqlUpdateTable, execution)
      case et: LocalEntities =>
        writeGenericLocalEntities(dataset, et, execution)
    }
  }

  private def writeGenericLocalEntities(dataset: Task[DatasetSpec[DatasetType]], et: LocalEntities, execution: LocalExecution)
                                       (implicit userContext: UserContext, context: ActivityContext[ExecutionReport], prefixes: Prefixes): Unit = {
    implicit val report: ExecutionReportUpdater = WriteEntitiesReportUpdater(dataset, context)
    withEntitySink(dataset, execution) { entitySink =>
      writeEntities(entitySink, et)
    }
    report.executionDone()
  }

  final val remainingSparqlUpdateQueryBufferSize = 1000

  case class SparqlUpdateExecutionReportUpdater(task: Task[TaskSpec], context: ActivityContext[ExecutionReport]) extends ExecutionReportUpdater {
    var remainingQueries = 0

    override def operationLabel: Option[String] = Some("generate queries")

    override def entityProcessVerb: String = "executed"

    override def entityLabelSingle: String = "Update query"

    override def entityLabelPlural: String = "Update queries"

    override def minEntitiesBetweenUpdates: Int = 1

    override def additionalFields(): Seq[(String, String)] = {
      if(remainingQueries > 0) {
        Seq(
          "Remaining queries" -> remainingQueriesStat,
          "Estimated runtime" -> estimatedRuntimeStat
        )
      } else {
        Seq.empty
      }
    }

    private def remainingQueriesStat: String = {
      if (remainingQueries >= remainingSparqlUpdateQueryBufferSize) {
        "> " + remainingSparqlUpdateQueryBufferSize
      } else {
        remainingQueries.toString
      }
    }

    private def estimatedRuntimeStat: String = {
      if (runtime > 0) {
        val estimation = String.format("%.2f", remainingQueries / throughput) + " seconds"
        if (remainingQueries >= remainingSparqlUpdateQueryBufferSize) {
          "> " + estimation
        } else {
          estimation
        }
      } else {
        "-"
      }
    }
  }

  private def executeSparqlUpdateQueries(dataset: Task[DatasetSpec[DatasetType]],
                                         sparqlUpdateTable: SparqlUpdateEntityTable,
                                         execution: LocalExecution)
                                        (implicit userContext: UserContext, context: ActivityContext[ExecutionReport], prefixes: Prefixes): Unit = {
    dataset.plugin match {
      case rdfDataset: RdfDataset =>
        val endpoint = rdfDataset.sparqlEndpoint
        val executionReport = SparqlUpdateExecutionReportUpdater(dataset, context)
        val queryBuffer = SparqlQueryBuffer(remainingSparqlUpdateQueryBufferSize, sparqlUpdateTable.entities)
        for (updateQuery <- queryBuffer) {
          endpoint.update(updateQuery)
          executionReport.increaseEntityCounter()
          executionReport.remainingQueries = queryBuffer.bufferedQuerySize
        }
        executionReport.executionDone()
      case _ =>
        writeGenericLocalEntities(dataset, sparqlUpdateTable, execution)
    }
  }

  /** Buffers queries to make prediction about how many queries will be executed.
    *
    * @param bufferSize max size of queries that should be buffered
    */
  case class SparqlQueryBuffer(queryBufferSize: Int, entities: CloseableIterator[Entity]) extends TraversableIterator[String] {
    private val queryBuffer = new util.LinkedList[String]()

    override def foreach[U](f: String => U): Unit = {
      entities foreach { entity =>
        assert(entity.values.size == 1 && entity.values.head.size == 1)
        val query = entity.values.head.head
        queryBuffer.push(query)
        if(queryBuffer.size() > queryBufferSize) {
          f(queryBuffer.remove())
        }
      }
      while(!queryBuffer.isEmpty) {
        f(queryBuffer.remove())
      }
    }

    def bufferedQuerySize: Int = queryBuffer.size()
  }

  private def uploadFilesViaGraphStore(dataset: Task[DatasetSpec[Dataset]],
                                       table: GraphStoreFileUploadTable,
                                       reportUpdater: ExecutionReportUpdater)
                                      (implicit userContext: UserContext): Unit = {
    val datasetLabelOrId = dataset.metaData.formattedLabel(dataset.id)
    dataset.data match {
      case datasetSpec: DatasetSpec[_] =>
        datasetSpec.plugin match {
          case rdfDataset: RdfDataset if rdfDataset.sparqlEndpoint.isInstanceOf[GraphStoreFileUploadTrait] =>
            val sparqlEndpoint = rdfDataset.sparqlEndpoint
            val targetGraph = sparqlEndpoint.sparqlParams.graph match {
              case Some(g) => g
              case None => throw new ValidationException(s"No graph defined on dataset $datasetLabelOrId of type '${dataset.plugin.pluginSpec.label}'!")
            }
            val graphStore = sparqlEndpoint.asInstanceOf[GraphStoreFileUploadTrait]
            for(fileResource <- table.files) {
              graphStore.uploadFileToGraph(targetGraph, fileResource.file, table.contentType, None)
              reportUpdater.increaseEntityCounter()
            }
          case _: Dataset =>
            throw new ValidationException(s"Dataset task ${dataset.id} of type ${datasetSpec.plugin.pluginSpec.label} " +
                s"has no support for graph store file uploads!")
        }
      case _ =>
        throw new ValidationException("No dataset spec found!")
    }
    reportUpdater.executionDone()
  }

  // Write the resource from the resource entity table to the dataset's resource
  private def writeDatasetResource(dataset: Task[DatasetSpec[Dataset]], datasetResource: DatasetResourceEntityTable): Unit = {
    val inputResource = datasetResource.datasetResource
    dataset.data match {
      case datasetSpec: DatasetSpec[_] =>
        datasetSpec.plugin match {
          case dsr: ResourceBasedDataset =>
            dsr.writableResource match {
              case Some(wr) =>
                wr.writeResource(inputResource)
              case None =>
                throw new ValidationException(s"Dataset task ${dataset.id} of type ${datasetSpec.plugin.pluginSpec.label} " +
                    s"does not have a writable resource!")
            }
          case _: Dataset =>
            throw new ValidationException(s"Dataset task ${dataset.id} of type ${datasetSpec.plugin.pluginSpec.label} " +
                s"has no resource (file) or does not support the required interface!")
        }
      case _ =>
        throw new ValidationException("No dataset spec found!")
    }
  }

  private def withLinkSink(dataset: Task[DatasetSpec[DatasetType]], execution: LocalExecution)(f: LinkSink => Unit)(implicit userContext: UserContext): Unit = {
    using(access(dataset, execution).linkSink)(f)
  }

  private def withEntitySink(dataset: Task[DatasetSpec[DatasetType]], execution: LocalExecution)(f: EntitySink => Unit)(implicit userContext: UserContext): Unit = {
    using(access(dataset, execution).entitySink)(f)
  }

  private def writeEntities(sink: EntitySink, entityTable: LocalEntities)
                           (implicit userContext: UserContext, prefixes: Prefixes, executionReport: ExecutionReportUpdater): Unit = {
    var entityCount = 0
    val startTime = System.currentTimeMillis()
    var lastLog = startTime
    sink.openTableWithPaths(entityTable.entitySchema.typeUri, entityTable.entitySchema.typedPaths, entityTable.entitySchema.singleEntity)
    for (entity <- entityTable.entities) {
      if(entityCount < ExecutionReport.SAMPLE_ENTITY_LIMIT) {
        executionReport.addSampleEntity(EntitySample.entityToEntitySample(entity))
      }
      executionReport.increaseEntityCounter()
      sink.writeEntity(entity.uri, entity.values)
      entityCount += 1
      if(entityCount % 10000 == 0) {
        val currentTime = System.currentTimeMillis()
        if(currentTime - 2000 > lastLog) {
          logger.info("Writing entities: " + entityCount)
          lastLog = currentTime
        }
      }
    }
    sink.closeTable()
    val time = (System.currentTimeMillis - startTime) / 1000.0
    logger.log(Level.INFO, "Finished writing " + entityCount + " entities with type '" + entityTable.entitySchema.typeUri + "' in " + time + " seconds")
  }

  private def writeLinks(dataset: Task[DatasetSpec[DatasetType]], sink: LinkSink, links: Seq[Link], linkType: Uri, inverseLinkType: Option[Uri])
                        (implicit userContext: UserContext, prefixes: Prefixes, context: ActivityContext[ExecutionReport]): Unit = {
    implicit val report: ExecutionReportUpdater = WriteLinksReportUpdater(dataset, context)
    val startTime = System.currentTimeMillis()
    sink.init()
    for (link <- links) {
      sink.writeLink(link, linkType.uri, inverseLinkType.map(_.uri))
      report.increaseEntityCounter()
    }
    val time = (System.currentTimeMillis - startTime) / 1000.0
    report.executionDone()
    logger.log(Level.INFO, "Finished writing links in " + time + " seconds")
  }

  private def writeTriples(sink: EntitySink, entities: CloseableIterator[Entity])
                          (implicit userContext: UserContext, prefixes: Prefixes): Unit = {
    sink match {
      case tripleSink: TripleSink =>
        writeTriples(entities, tripleSink)
      case EntitySinkWrapper(tripleSink: TripleSink, _) =>
        writeTriples(entities, tripleSink)
      case _ =>
        throw TaskException("Cannot write triples to non-RDF dataset!")
    }
  }

  private def writeTriples(entities: CloseableIterator[Entity], sink: TripleSink)
                          (implicit userContext: UserContext, prefixes: Prefixes): Unit = {
    sink.init()
    for (entity <- entities) {
      if (entity.values.size < 4 || entity.values.size > 5) {
        throw new scala.RuntimeException("TripleEntityTable with invalid Entity encountered.")
      }
      try {
        entity.schema match{
          case TripleEntityTable.schema =>
            val Seq(s, p, o, encodedType) = entity.values.map(_.head)
            val valueType = TripleEntityTable.convertToValueType(encodedType)
            sink.writeTriple(s, p, o, valueType)
          case QuadEntityTable.schema =>
            val Seq(s, p, o, encodedType, context) = entity.values.map(_.head)
            val valueType = TripleEntityTable.convertToValueType(encodedType)
            sink.writeTriple(s, p, o, valueType)  //FIXME CMEM-1759 quad context is ignored for now, change when quad sink is available
          case _ =>
            throw new scala.RuntimeException("Unexpected entity schema format: " + entity.schema)
        }
      } catch {
        case ex: Exception =>
          throw new scala.RuntimeException("Triple entity with empty values", ex)
      }
    }
  }

  private def writeMultiTables(sink: EntitySink, tables: MultiEntityTable)
                              (implicit userContext: UserContext, prefixes: Prefixes, executionReport: ExecutionReportUpdater): Unit = {
    for(table <- tables.allTables) {
      writeEntities(sink, table)
    }
  }

  case class WriteEntitiesReportUpdater(task: Task[TaskSpec], context: ActivityContext[ExecutionReport]) extends ExecutionReportUpdater {
    override def operationLabel: Option[String] = Some("write")
    override def entityProcessVerb: String = "written"
  }

  case class ReadEntitiesReportUpdater(task: Task[TaskSpec], context: ActivityContext[ExecutionReport]) extends ExecutionReportUpdater {
    override def operationLabel: Option[String] = Some("read")
    override def entityProcessVerb: String = "read"
  }

  case class WriteLinksReportUpdater(task: Task[TaskSpec], context: ActivityContext[ExecutionReport]) extends ExecutionReportUpdater {
    override def entityLabelSingle: String = "Link"
    override def entityLabelPlural: String = "Links"
    override def operationLabel: Option[String] = Some("write")
    override def entityProcessVerb: String = "written"
  }

  case class UploadFilesViaGspReportUpdater(task: Task[TaskSpec], context: ActivityContext[ExecutionReport]) extends ExecutionReportUpdater {
    override def entityLabelSingle: String = "File"

    override def entityLabelPlural: String = "Files"

    override def operationLabel: Option[String] = Some("upload")

    override def entityProcessVerb: String = "uploaded"
  }
}

// To be used in cases when no specific LocalDatasetExecutor has been implemented yet for a particular dataset type.
class GenericLocalDatasetExecutor extends LocalDatasetExecutor[Dataset]
