package org.silkframework.execution.local

import java.util.logging.{Level, Logger}

import org.silkframework.config.Task
import org.silkframework.dataset.rdf._
import org.silkframework.dataset.{Dataset, TripleSinkDataset}
import org.silkframework.entity._
import org.silkframework.execution.{DatasetExecutor, TaskException}
import org.silkframework.util.Uri

/**
  * Created on 7/20/16.
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
  override def read(dataset: Task[Dataset], schema: EntitySchema): EntityTable = {
    schema match {
      case TripleEntitySchema.schema =>
        dataset.data match {
          case rdfDataset: RdfDataset =>
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
              new Entity(s, IndexedSeq(Seq(s), Seq(p), Seq(value), Seq(typ)), TripleEntitySchema.schema)
            }
            TripleEntityTable(tripleEntities, dataset)
          case _ =>
            throw TaskException("Dataset is not a RDF dataset and thus cannot output triples!")
        }
      case _ =>
        val entities = dataset.source.retrieve(entitySchema = schema)
        GenericEntityTable(entities, entitySchema = schema, dataset)
    }
  }

  override protected def write(data: EntityTable, dataset: Task[Dataset]): Unit = {
    data match {
      case LinksTable(links, linkType, _) =>
        writeLinks(dataset, links, linkType)
      case TripleEntityTable(entities, _) =>
        writeTriples(dataset, entities)
      case tables: MultiEntityTable =>
        writeMultiTables(dataset, tables)
      case et: EntityTable =>
        writeEntities(dataset, et)
    }
  }

  private def writeEntities(dataset: Dataset, entityTable: EntityTable): Unit = {
    var entityCount = 0
    val startTime = System.currentTimeMillis()
    var lastLog = startTime
    val sink = dataset.entitySink
    sink.openWithTypedPath(entityTable.entitySchema.typedPaths)
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
    sink.close()
    val time = (System.currentTimeMillis - startTime) / 1000.0
    logger.log(Level.INFO, "Finished writing " + entityCount + " entities with type '" + entityTable.entitySchema.typeUri + "' in " + time + " seconds")
  }

  private def writeLinks(dataset: Dataset, links: Seq[Link], linkType: Uri): Unit = {
    val startTime = System.currentTimeMillis()
    val sink = dataset.linkSink
    sink.writeLinks(links, linkType.uri)
    val time = (System.currentTimeMillis - startTime) / 1000.0
    logger.log(Level.INFO, "Finished writing links in " + time + " seconds")
  }

  private def writeTriples(dataset: Dataset, entities: Traversable[Entity]): Unit = {
    dataset match {
      case rdfDataset: TripleSinkDataset =>
        writeTriples(entities, rdfDataset)
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

  private def writeTriples(entities: Traversable[Entity], tripleSinkDataset: TripleSinkDataset): Unit = {
    val sink = tripleSinkDataset.tripleSink
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
    sink.close()
  }

  private def writeMultiTables(dataset: Dataset, tables: MultiEntityTable): Unit = {
    writeEntities(dataset, tables)
    for(table <- tables.subTables) {
      writeEntities(dataset, table)
    }
  }

}
