package org.silkframework.execution.local

import org.silkframework.config.{Prefixes, Task, TaskSpec}
import org.silkframework.dataset.CloseableDataset.using
import org.silkframework.dataset.DatasetSpec.{EntitySinkWrapper, GenericDatasetSpec}
import org.silkframework.dataset._
import org.silkframework.dataset.bulk.{BulkResourceBasedDataset, ZipWritableResource}
import org.silkframework.dataset.rdf._
import org.silkframework.entity._
import org.silkframework.execution._
import org.silkframework.execution.typed._
import org.silkframework.runtime.activity.{ActivityContext, UserContext}
import org.silkframework.runtime.iterator.{CloseableIterator, TraversableIterator}
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.resource.zip.ZipOutputStreamResource
import org.silkframework.runtime.resource.{FileResource, ReadOnlyResource, WritableResource}
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.Uri

import java.util
import java.util.logging.{Level, Logger}
import java.util.zip.ZipOutputStream
import scala.util.Using

/**
  * Local dataset executor that handles read and writes to [[Dataset]] tasks.
  */
abstract class LocalDatasetExecutor[DatasetType <: Dataset] extends DatasetExecutor[DatasetType, LocalExecution] {
  private val logger = Logger.getLogger(getClass.getName)

  /**
    * Reads data from a dataset.
    */
  override def read(dataset: Task[DatasetSpec[DatasetType]], schema: EntitySchema, execution: LocalExecution)
                   (implicit pluginContext: PluginContext, context: ActivityContext[ExecutionReport]): LocalEntities = {
    implicit val prefixes: Prefixes = pluginContext.prefixes
    implicit val user: UserContext = pluginContext.user

    //FIXME CMEM-1759 clean this and use only plugin based implementations of LocalEntities
    lazy val source = access(dataset, execution).source
    schema match {
      case EmptyEntityTable.schema =>
        EmptyEntityTable(dataset)
      case QuadEntitySchema.schema =>
        handleQuadEntitySchema(dataset)
      case SparqlEndpointEntitySchema.schema =>
        handleSparqlEndpointSchema(dataset)
      case multi: MultiEntitySchema =>
        handleMultiEntitySchema(dataset, source, schema, multi)
      case FileEntitySchema.schema =>
        handleDatasetResourceEntitySchema(dataset)
      case _ =>
        implicit val executionReport: ExecutionReportUpdater = ReadEntitiesReportUpdater(dataset, context)
        val table = source.retrieve(entitySchema = schema)
        GenericEntityTable(ReportingIterator(table.entities), entitySchema = schema, dataset, table.globalErrors)
    }
  }

  private def handleDatasetResourceEntitySchema(dataset: Task[DatasetSpec[DatasetType]])
                                               (implicit pluginContext: PluginContext, context: ActivityContext[ExecutionReport]): LocalEntities = {
    dataset.data match {
      case datasetSpec: DatasetSpec[_] =>
        datasetSpec.plugin match {
          case dsr: ResourceBasedDataset =>
            implicit val executionReport: ExecutionReportUpdater = ReadFilesReportUpdater(dataset, context)
            implicit val prefixes: Prefixes = pluginContext.prefixes
            val fileEntities =
              for(resource <- BulkResourceBasedDataset.resources(dsr)) yield {
                FileEntity(ReadOnlyResource(resource), FileType.Project, dsr.mimeType)
              }
            ReportingIterator.addReporter(FileEntitySchema.create(fileEntities, dataset))
          case _: Dataset =>
            throw new ValidationException(s"Dataset task ${dataset.id} of type " +
                s"${datasetSpec.plugin.pluginSpec.label} has no resource (file) or does not support requests for its resource!")
        }
      case _ =>
        throw new ValidationException("No dataset spec found!")
    }
  }

  private def handleMultiEntitySchema(dataset: Task[DatasetSpec[Dataset]], source: DataSource, schema: EntitySchema, multi: MultiEntitySchema)
                                     (implicit pluginContext: PluginContext, context: ActivityContext[ExecutionReport])= {
    implicit val executionReport: ExecutionReportUpdater = ReadEntitiesReportUpdater(dataset, context)
    implicit val prefixes: Prefixes = pluginContext.prefixes
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

  private def handleQuadEntitySchema(dataset: Task[DatasetSpec[Dataset]])
                                    (implicit pluginContext: PluginContext): TypedEntities[Quad, TaskSpec] = {
    dataset.data match {
      case rdfDataset: RdfDataset =>
        readTriples(dataset, rdfDataset)
      case DatasetSpec(rdfDataset: RdfDataset, _, _) =>
        readTriples(dataset, rdfDataset)
      case _ =>
        throw TaskException("Dataset is not a RDF dataset and thus cannot output triples!")
    }
  }

  private def readTriples(dataset: Task[GenericDatasetSpec], rdfDataset: RdfDataset)
                       (implicit pluginContext: PluginContext): TypedEntities[Quad, TaskSpec] = {
    val sparqlResult = rdfDataset.sparqlEndpoint.select("SELECT ?s ?p ?o WHERE {?s ?p ?o}")(pluginContext.user)
    val tripleEntities = sparqlResult.bindings map { resultMap =>
      val s = resultMap("s").asInstanceOf[ConcreteNode]
      val p = resultMap("p").asInstanceOf[Resource]
      val v = resultMap("o")
      Triple(s, p, v)
    }
    QuadEntitySchema.create(tripleEntities, dataset)
  }


  private def handleSparqlEndpointSchema(dataset: Task[GenericDatasetSpec])(implicit pluginContext: PluginContext): TypedEntities[Unit, DatasetSpec[RdfDataset]] = {
    dataset.data match {
      case DatasetSpec(_: RdfDataset, _, _) =>
        SparqlEndpointEntitySchema.create(dataset.asInstanceOf[Task[DatasetSpec[RdfDataset]]])
      case _ =>
        throw TaskException("Dataset does not offer a SPARQL endpoint!")
    }
  }

  override protected def write(data: LocalEntities, dataset: Task[DatasetSpec[DatasetType]], execution: LocalExecution)
                              (implicit pluginContext: PluginContext, context: ActivityContext[ExecutionReport]): Unit = {
    implicit val prefixes: Prefixes = pluginContext.prefixes
    implicit val user: UserContext = pluginContext.user

    DatasetSpec.checkDatasetAllowsWriteAccess(Some(dataset.fullLabel), dataset.readOnly)

    data match {
      case LinksEntitySchema(links) =>
        withLinkSink(dataset, execution) { linkSink =>
          writeLinks(dataset, linkSink, links.typedEntities, links.task.linkType, links.task.inverseLinkType)
        }
      case QuadEntitySchema(quads) =>
        withEntitySink(dataset, execution) { entitySink =>
          writeQuads(entitySink, quads, WriteTriplesReportUpdater(dataset, context))
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
      case FileEntitySchema(files) if dataset.data.plugin.isInstanceOf[ResourceBasedDataset] =>
        writeDatasetResource(dataset, files, WriteFilesReportUpdater(dataset, context), resource => execution.addUpdatedFile(resource.name))
      case FileEntitySchema(files) if dataset.data.plugin.isInstanceOf[RdfDataset] =>
        uploadFilesViaGraphStore(dataset, files.typedEntities, UploadFilesViaGspReportUpdater(dataset, context))
      case SparqlUpdateEntitySchema(queries) =>
        executeSparqlUpdateQueries(dataset, queries, execution)
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
                                         sparqlUpdateQueries: TypedEntities[String, TaskSpec],
                                         execution: LocalExecution)
                                        (implicit userContext: UserContext, context: ActivityContext[ExecutionReport], prefixes: Prefixes): Unit = {
    dataset.plugin match {
      case rdfDataset: RdfDataset =>
        val endpoint = rdfDataset.sparqlEndpoint
        val executionReport = SparqlUpdateExecutionReportUpdater(dataset, context)
        val queryBuffer = SparqlQueryBuffer(remainingSparqlUpdateQueryBufferSize, sparqlUpdateQueries.typedEntities)
        for (updateQuery <- queryBuffer) {
          endpoint.update(updateQuery)
          executionReport.increaseEntityCounter()
          executionReport.remainingQueries = queryBuffer.bufferedQuerySize
        }
        executionReport.executionDone()
      case _ =>
        writeGenericLocalEntities(dataset, sparqlUpdateQueries, execution)
    }
  }

  /** Buffers queries to make prediction about how many queries will be executed.
    *
    * @param bufferSize max size of queries that should be buffered
    */
  case class SparqlQueryBuffer(queryBufferSize: Int, entities: CloseableIterator[String]) extends TraversableIterator[String] {
    private val queryBuffer = new util.LinkedList[String]()

    override def foreach[U](f: String => U): Unit = {
      entities foreach { query =>
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
                                       files: CloseableIterator[FileEntity],
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
            for(fileEntity <- files) {
              val file = fileEntity.file match {
                case FileResource(file) => file
                case _ => throw new ValidationException(s"Cannot upload non-local file to GraphStore: $fileEntity")
              }
              val mimeType = fileEntity.mimeType.getOrElse(throw new ValidationException(s"Cannot upload file to GraphStore that is missing the MIME type: $fileEntity"))
              graphStore.uploadFileToGraph(targetGraph, file, mimeType, None)
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

  // Write file entities to the dataset's resource
  private def writeDatasetResource(dataset: Task[DatasetSpec[Dataset]],
                                   fileEntities: TypedEntities[FileEntity, TaskSpec],
                                   reportUpdater: ExecutionReportUpdater,
                                   onUpdate: WritableResource => Unit): Unit = {
    dataset.data match {
      case datasetSpec: DatasetSpec[_] =>
        datasetSpec.plugin match {
          case dsr: ResourceBasedDataset =>
            dsr.writableResource match {
              case Some(outputResource) =>
                if(writeResources(fileEntities.typedEntities, outputResource, reportUpdater)) {
                  onUpdate(outputResource)
                }
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
    reportUpdater.executionDone()
  }

  private def writeResources(fileEntities: Iterator[FileEntity], outputResource: WritableResource, reportUpdater: ExecutionReportUpdater): Boolean = {
    var resourceWritten = false
    if(!BulkResourceBasedDataset.isZip(outputResource)) {
      // We are writing to a non-zip resource, so we can only write one file
      for(inputResource <- fileEntities) {
        if(BulkResourceBasedDataset.isZip(inputResource.file)) {
          throw new ValidationException(s"Cannot write a zip file (${inputResource.file.name}) to a dataset that's not based on a zip file.")
        }
        if(resourceWritten) {
          throw new ValidationException(s"Cannot write multiple files to a dataset that is not based on a zip file: ${outputResource.name}")
        }
        outputResource.writeResource(inputResource.file)
        resourceWritten = true
        reportUpdater.increaseEntityCounter()
      }
    } else {
      // We are writing to a zip resource
      fileEntities.nextOption() match {
        case None =>
          // No files to write, nothing to do
        case Some(firstEntity) if BulkResourceBasedDataset.isZip(firstEntity.file) && !fileEntities.hasNext =>
          // If there is only one file and it is a zip file, we can write it directly
          outputResource.writeResource(firstEntity.file)
        case Some(firstEntity) =>
          // Otherwise we package all files into a zip file
          outputResource.write() { outputStream =>
            Using.resource(new ZipOutputStream(outputStream)) { zipOutput =>
              for (inputResource <- Iterator(firstEntity) ++ fileEntities) {
                val entryResource = ZipOutputStreamResource(inputResource.file.name, inputResource.file.name, zipOutput)
                entryResource.writeResource(inputResource.file)
                resourceWritten = true
                reportUpdater.increaseEntityCounter()
              }
            }
          }
      }
    }
    resourceWritten
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

  private def writeLinks(dataset: Task[DatasetSpec[DatasetType]], sink: LinkSink, links: CloseableIterator[Link], linkType: Uri, inverseLinkType: Option[Uri])
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

  private def writeQuads(sink: EntitySink, quads: TypedEntities[Quad, TaskSpec], reportUpdater: WriteTriplesReportUpdater)
                        (implicit userContext: UserContext, prefixes: Prefixes): Unit = {
    sink match {
      case tripleSink: TripleSink =>
        writeQuadsToTripleSink(tripleSink, quads, reportUpdater)
      case EntitySinkWrapper(tripleSink: TripleSink, _) =>
        writeQuadsToTripleSink(tripleSink, quads, reportUpdater)
      case _ =>
        // Write the statements as generic entities
        implicit val executionReportUpdater: ExecutionReportUpdater = reportUpdater
        writeEntities(sink, quads)
    }
  }

  private def writeQuadsToTripleSink(sink: TripleSink, quads: TypedEntities[Quad, TaskSpec], reportUpdater: WriteTriplesReportUpdater)
                                    (implicit userContext: UserContext, prefixes: Prefixes): Unit = {
    sink.init()
    for (quad <- quads.typedEntities) {
      sink.writeTriple(quad.subject.value, quad.predicate.value, quad.objectVal.value, QuadEntitySchema.getValueType(quad.objectVal))
      reportUpdater.increaseEntityCounter()
    }
    reportUpdater.executionDone()
  }

  private def writeMultiTables(sink: EntitySink, tables: MultiEntityTable)
                              (implicit userContext: UserContext, prefixes: Prefixes, executionReport: ExecutionReportUpdater): Unit = {
    for(table <- tables.allTables) {
      writeEntities(sink, table)
    }
  }

  private case class WriteEntitiesReportUpdater(task: Task[TaskSpec], context: ActivityContext[ExecutionReport]) extends ExecutionReportUpdater {
    override def operationLabel: Option[String] = Some("write")
    override def entityProcessVerb: String = "written"
  }

  private case class ReadEntitiesReportUpdater(task: Task[TaskSpec], context: ActivityContext[ExecutionReport]) extends ExecutionReportUpdater {
    override def operationLabel: Option[String] = Some("read")
    override def entityProcessVerb: String = "read"
  }

  private case class ReadFilesReportUpdater(task: Task[TaskSpec], context: ActivityContext[ExecutionReport]) extends ExecutionReportUpdater {
    override def entityLabelSingle: String = "File"
    override def entityLabelPlural: String = "Files"
    override def operationLabel: Option[String] = Some("read")
    override def entityProcessVerb: String = "read"
  }

  private case class WriteLinksReportUpdater(task: Task[TaskSpec], context: ActivityContext[ExecutionReport]) extends ExecutionReportUpdater {
    override def entityLabelSingle: String = "Link"
    override def entityLabelPlural: String = "Links"
    override def operationLabel: Option[String] = Some("write")
    override def entityProcessVerb: String = "written"
  }

  private case class WriteFilesReportUpdater(task: Task[TaskSpec], context: ActivityContext[ExecutionReport]) extends ExecutionReportUpdater {
    override def entityLabelSingle: String = "File"
    override def entityLabelPlural: String = "Files"
    override def operationLabel: Option[String] = Some("write")
    override def entityProcessVerb: String = "written"
  }

  private case class UploadFilesViaGspReportUpdater(task: Task[TaskSpec], context: ActivityContext[ExecutionReport]) extends ExecutionReportUpdater {
    override def entityLabelSingle: String = "File"
    override def entityLabelPlural: String = "Files"
    override def operationLabel: Option[String] = Some("upload")
    override def entityProcessVerb: String = "uploaded"
  }

  case class WriteTriplesReportUpdater(task: Task[TaskSpec], context: ActivityContext[ExecutionReport]) extends ExecutionReportUpdater {
    override def entityLabelSingle: String = "Triple"
    override def entityLabelPlural: String = "Triples"
    override def operationLabel: Option[String] = Some("write")
    override def entityProcessVerb: String = "written"
  }
}

// To be used in cases when no specific LocalDatasetExecutor has been implemented yet for a particular dataset type.
class GenericLocalDatasetExecutor extends LocalDatasetExecutor[Dataset]
