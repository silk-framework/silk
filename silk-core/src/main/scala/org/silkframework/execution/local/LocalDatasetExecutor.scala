package org.silkframework.execution.local

import java.util
import java.util.logging.{Level, Logger}

import org.silkframework.config.{Prefixes, Task}
import org.silkframework.dataset.DatasetSpec.{EntitySinkWrapper, GenericDatasetSpec}
import org.silkframework.dataset.rdf._
import org.silkframework.dataset._
import org.silkframework.entity._
import org.silkframework.execution.{DatasetExecutor, ExecutionReport, ExecutionReportUpdater, TaskException}
import org.silkframework.runtime.activity.{ActivityContext, UserContext}
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.Uri
import CloseableDataset.using
import scala.util.control.NonFatal

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
        val entities = source.retrieve(entitySchema = schema)
        GenericEntityTable(entities, entitySchema = schema, dataset)
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
                                     (implicit userContext: UserContext)= {
    MultiEntityTable(
      entities = source.retrieve(entitySchema = schema),
      entitySchema = schema,
      subTables =
          for (subSchema <- multi.subSchemata) yield
            GenericEntityTable(source.retrieve(entitySchema = subSchema), subSchema, dataset),
      task = dataset
    )
  }

  private def handleTripleEntitySchema(dataset: Task[DatasetSpec[Dataset]])
                                      (implicit userContext: UserContext): TripleEntityTable = {
    dataset.data match {
      case rdfDataset: RdfDataset =>
        readTriples(dataset, rdfDataset)
      case DatasetSpec(rdfDataset: RdfDataset, _) =>
        readTriples(dataset, rdfDataset)
      case _ =>
        throw TaskException("Dataset is not a RDF dataset and thus cannot output triples!")
    }
  }
  private def handleSparqlEndpointSchema(dataset: Task[GenericDatasetSpec]): SparqlEndpointEntityTable = {
    dataset.data match {
      case rdfDataset: RdfDataset =>
        new SparqlEndpointEntityTable(rdfDataset.sparqlEndpoint, dataset)
      case DatasetSpec(rdfDataset: RdfDataset, _) =>
        new SparqlEndpointEntityTable(rdfDataset.sparqlEndpoint, dataset)
      case _ =>
        throw TaskException("Dataset does not offer a SPARQL endpoint!")
    }
  }

  private def readTriples(dataset: Task[GenericDatasetSpec], rdfDataset: RdfDataset)
                         (implicit userContext: UserContext): TripleEntityTable = {
    val sparqlResult = rdfDataset.sparqlEndpoint.select("SELECT ?s ?p ?o WHERE {?s ?p ?o}")
    val tripleEntities = sparqlResult.bindings.view map { resultMap =>
      val s = resultMap("s").value
      val p = resultMap("p").value
      val (value, typ) = TripleEntityTable.convertToEncodedType(resultMap("o"))
      Entity(s, IndexedSeq(Seq(s), Seq(p), Seq(value), Seq(typ)), TripleEntityTable.schema)
    }
    new TripleEntityTable(tripleEntities, dataset)
  }

  override protected def write(data: LocalEntities, dataset: Task[DatasetSpec[DatasetType]], execution: LocalExecution)
                              (implicit userContext: UserContext, context: ActivityContext[ExecutionReport], prefixes: Prefixes): Unit = {
    //FIXME CMEM-1759 clean this and use only plugin based implementations of LocalEntities
    data match {
      case LinksTable(links, linkType, _) =>
        withLinkSink(dataset, execution) { linkSink =>
          writeLinks(linkSink, links, linkType)
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
        withEntitySink(dataset, execution) { entitySink =>
          writeMultiTables(entitySink, tables)
        }
      case datasetResource: DatasetResourceEntityTable =>
        writeDatasetResource(dataset, datasetResource)
      case sparqlUpdateTable: SparqlUpdateEntityTable =>
        executeSparqlUpdateQueries(dataset, sparqlUpdateTable)
      case et: LocalEntities =>
        withEntitySink(dataset, execution) { entitySink =>
          writeEntities(entitySink, et)
        }
    }
  }

  final val remainingSparqlUpdateQueryBufferSize = 1000

  case class SparqlUpdateExecutionReportUpdater(taskLabel: String, context: ActivityContext[ExecutionReport]) extends ExecutionReportUpdater {
    var remainingQueries = 0

    override def entityProcessVerb: String = "executed"

    override def entityLabelSingle: String = "Update query"

    override def entityLabelPlural: String = "Update queries"

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
        val estimation = (remainingQueries / throughput).formatted("%.2f") + " seconds"
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

  private def executeSparqlUpdateQueries(dataset: Task[DatasetSpec[Dataset]],
                                         sparqlUpdateTable: SparqlUpdateEntityTable)
                                        (implicit userContext: UserContext, context: ActivityContext[ExecutionReport]): Unit = {
    dataset.plugin match {
      case rdfDataset: RdfDataset =>
        val endpoint = rdfDataset.sparqlEndpoint
        val executionReport = SparqlUpdateExecutionReportUpdater(dataset.taskLabel(), context)
        val queryBuffer = SparqlQueryBuffer(remainingSparqlUpdateQueryBufferSize, sparqlUpdateTable.entities)
        for (updateQuery <- queryBuffer) {
          endpoint.update(updateQuery)
          executionReport.increaseEntityCounter()
          executionReport.remainingQueries = queryBuffer.bufferedQuerySize
          executionReport.update()
        }
        executionReport.update(force = true, addEndTime = true)
      case _ =>
        throw new ValidationException(s"Dataset task ${dataset.id} is not an RDF dataset!")
    }
  }

  /** Buffers queries to make prediction about how many queries will be executed.
    *
    * @param bufferSize max size of queries that should be buffered
    */
  case class SparqlQueryBuffer(bufferSize: Int, entities: Traversable[Entity]) extends Traversable[String] {
    private val queryBuffer = new util.LinkedList[String]()

    override def foreach[U](f: String => U): Unit = {
      entities foreach { entity =>
        assert(entity.values.size == 1 && entity.values.head.size == 1)
        val query = entity.values.head.head
        queryBuffer.push(query)
        if(queryBuffer.size() > bufferSize) {
          f(queryBuffer.remove())
        }
      }
      while(!queryBuffer.isEmpty) {
        f(queryBuffer.remove())
      }
    }

    def bufferedQuerySize: Int = queryBuffer.size()
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
                           (implicit userContext: UserContext, prefixes: Prefixes): Unit = {
    var entityCount = 0
    val startTime = System.currentTimeMillis()
    var lastLog = startTime
    sink.openWithEntitySchema(entityTable.entitySchema)
    for (entity <- entityTable.entities) {
      sink.writeEntity(entity)
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

  private def writeLinks(sink: LinkSink, links: Seq[Link], linkType: Uri)
                        (implicit userContext: UserContext): Unit = {
    val startTime = System.currentTimeMillis()
    sink.init()
    for (link <- links) sink.writeLink(link, linkType.uri)
    val time = (System.currentTimeMillis - startTime) / 1000.0
    logger.log(Level.INFO, "Finished writing links in " + time + " seconds")
  }

  private def writeTriples(sink: EntitySink, entities: Traversable[Entity])
                          (implicit userContext: UserContext): Unit = {
    sink match {
      case tripleSink: TripleSink =>
        writeTriples(entities, tripleSink)
      case EntitySinkWrapper(tripleSink: TripleSink, __) =>
        writeTriples(entities, tripleSink)
      case _ =>
        throw TaskException("Cannot write triples to non-RDF dataset!")
    }
  }

  private def writeTriples(entities: Traversable[Entity], sink: TripleSink)
                          (implicit userContext: UserContext): Unit = {
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
        }
      } catch {
        case e: Exception =>
          throw new scala.RuntimeException("Triple entity with empty values")
      }
    }
  }

  private def writeMultiTables(sink: EntitySink, tables: MultiEntityTable)
                              (implicit userContext: UserContext, prefixes: Prefixes): Unit = {
    writeEntities(sink, tables)
    for(table <- tables.subTables) {
      writeEntities(sink, table)
    }
  }
}

// To be used in cases when no specific LocalDatasetExecutor has been implemented yet for a particular dataset type.
class GenericLocalDatasetExecutor extends LocalDatasetExecutor[Dataset]
