package org.silkframework.execution.local

import java.util.logging.{Level, Logger}

import org.silkframework.config.Task
import org.silkframework.dataset.DatasetSpec.{EntitySinkWrapper, GenericDatasetSpec}
import org.silkframework.dataset.rdf._
import org.silkframework.dataset._
import org.silkframework.entity._
import org.silkframework.execution.{DatasetExecutor, TaskException}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.Uri

/**
  * Local dataset executor that handles read and writes to [[Dataset]] tasks.
  */
class LocalDatasetExecutor extends DatasetExecutor[Dataset, LocalExecution] {
  private val logger = Logger.getLogger(getClass.getName)

  final val LANGUAGE_ENC_PREFIX = "lg"
  final val DATA_TYPE_ENC_PREFIX = "dt"
  final val URI_ENC_PREFIX = "ur"
  final val BLANK_NODE_ENC_PREFIX = "bn"

  /**
    * Reads data from a dataset.
    */
  override def read(dataset: Task[DatasetSpec[Dataset]], schema: EntitySchema, execution: LocalExecution)
                   (implicit userContext: UserContext): LocalEntities = {
    schema match {
      case TripleEntitySchema.schema =>
        handleTripleEntitySchema(dataset)
      case SparqlEndpointEntitySchema.schema =>
        handleSparqlEndpointSchema(dataset)
      case multi: MultiEntitySchema =>
        handleMultiEntitySchema(dataset, schema, multi)
      case DatasetResourceEntitySchema.schema =>
        handleDatasetResourceEntitySchema(dataset)
      case _ =>
        val entities = dataset.source.retrieve(entitySchema = schema)
        GenericEntityTable(entities, entitySchema = schema, dataset)
    }
  }

  private def handleDatasetResourceEntitySchema(dataset: Task[DatasetSpec[Dataset]]) = {
    dataset.data match {
      case datasetSpec: DatasetSpec[_] =>
        datasetSpec.plugin match {
          case dsr: ResourceBasedDataset =>
            new DatasetResourceEntityTable(dsr.file, dataset)
          case _: Dataset =>
            throw new ValidationException(s"Dataset task ${dataset.id} of type " +
                s"${datasetSpec.plugin.pluginSpec.label} has no resource (file) or does not support requests for its resource!")
        }
      case _ =>
        throw new ValidationException("No dataset spec found!")
    }
  }

  private def handleMultiEntitySchema(dataset: Task[DatasetSpec[Dataset]], schema: EntitySchema, multi: MultiEntitySchema)
                                     (implicit userContext: UserContext)= {
    MultiEntityTable(
      entities = dataset.source.retrieve(entitySchema = schema),
      entitySchema = schema,
      subTables =
          for (subSchema <- multi.subSchemata) yield
            GenericEntityTable(dataset.source.retrieve(entitySchema = subSchema), subSchema, dataset),
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
                         (implicit userContext: UserContext)= {
    val sparqlResult = rdfDataset.sparqlEndpoint.select("SELECT ?s ?p ?o WHERE {?s ?p ?o}")
    val tripleEntities = sparqlResult.bindings.view map { resultMap =>
      val s = resultMap("s").value
      val p = resultMap("p").value
      val (value, typ) = resultMap("o") match {
        case PlainLiteral(v) =>
          (v, "")
        case LanguageLiteral(v, l) =>
          (v, s"$LANGUAGE_ENC_PREFIX=$l")
        case DataTypeLiteral(v, dt) =>
          (v, s"$DATA_TYPE_ENC_PREFIX=$dt")
        case BlankNode(bn) =>
          (bn, s"$BLANK_NODE_ENC_PREFIX")
        case Resource(uri) =>
          (uri, s"$URI_ENC_PREFIX")
      }
      Entity(s, IndexedSeq(Seq(s), Seq(p), Seq(value), Seq(typ)), TripleEntitySchema.schema)
    }
    TripleEntityTable(tripleEntities, dataset)
  }

  override protected def write(data: LocalEntities, dataset: Task[DatasetSpec[Dataset]], execution: LocalExecution)
                              (implicit userContext: UserContext): Unit = {
    data match {
      case LinksTable(links, linkType, _) =>
        withLinkSink(dataset.data.plugin) { linkSink =>
          writeLinks(linkSink, links, linkType)
        }
      case TripleEntityTable(entities, _) =>
        withEntitySink(dataset) { entitySink =>
          writeTriples(entitySink, entities)
        }
      case tables: MultiEntityTable =>
        withEntitySink(dataset) { entitySink =>
          writeMultiTables(entitySink, tables)
        }
      case datasetResource: DatasetResourceEntityTable =>
        writeDatasetResource(dataset, datasetResource)
      case et: LocalEntities =>
        withEntitySink(dataset) { entitySink =>
          writeEntities(entitySink, et)
        }
    }
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

  private def withLinkSink(dataset: Dataset)(f: LinkSink => Unit)(implicit userContext: UserContext): Unit = {
    val sink = dataset.linkSink
    try {
      f(sink)
    } finally {
      sink.close()
    }
  }

  private def withEntitySink(dataset: DatasetSpec[Dataset])(f: EntitySink => Unit)(implicit userContext: UserContext): Unit = {
    val sink = dataset.entitySink
    try {
      f(sink)
    } finally {
     sink.close()
    }
  }

  private def writeEntities(sink: EntitySink, entityTable: LocalEntities)
                           (implicit userContext: UserContext): Unit = {
    var entityCount = 0
    val startTime = System.currentTimeMillis()
    var lastLog = startTime
    sink.openTableWithPaths(entityTable.entitySchema.typeUri, entityTable.entitySchema.typedPaths)
    for (entity <- entityTable.entities) {
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

  def convertToValueType(encodedType: String): ValueType = {
    encodedType.take(2) match {
      case DATA_TYPE_ENC_PREFIX =>
        CustomValueType(encodedType.drop(3))
      case LANGUAGE_ENC_PREFIX =>
        LanguageValueType(encodedType.drop(3))
      case URI_ENC_PREFIX =>
        UriValueType
      case BLANK_NODE_ENC_PREFIX =>
        BlankNodeValueType
      case _ =>
        StringValueType
    }
  }

  private def writeTriples(entities: Traversable[Entity], sink: TripleSink)
                          (implicit userContext: UserContext): Unit = {
    sink.init()
    for (entity <- entities) {
      if (entity.values.size != 4) {
        throw new scala.RuntimeException("Did not get exactly 4 values for triple entity!")
      }
      try {
        val Seq(s, p, o, encodedType) = entity.values.map(_.head)
        val valueType = convertToValueType(encodedType)
        sink.writeTriple(s, p, o, valueType)
      } catch {
        case e: Exception =>
          throw new scala.RuntimeException("Triple entity with empty values")
      }
    }
  }

  private def writeMultiTables(sink: EntitySink, tables: MultiEntityTable)
                              (implicit userContext: UserContext): Unit = {
    writeEntities(sink, tables)
    for(table <- tables.subTables) {
      writeEntities(sink, table)
    }
  }

}
