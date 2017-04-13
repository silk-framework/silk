package org.silkframework.execution.local

import java.util.logging.{Level, Logger}

import org.silkframework.config.Task
import org.silkframework.dataset.rdf._
import org.silkframework.dataset.{Dataset, NestedDataSource, TripleSinkDataset}
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
  override def read(dataset: Task[Dataset], schema: SchemaTrait): EntityTable[EntityTrait, SchemaTrait] = {
    schema match {
      case TripleEntitySchema.schema =>
        dataset.data match {
          case rdfDataset: RdfDataset =>
            readTriples(dataset, rdfDataset)
          case _ =>
            throw TaskException("Dataset is not a RDF dataset and thus cannot output triples!")
        }
      case nestedEntitySchema: NestedEntitySchema =>
        dataset.source match {
          case nestedSource: NestedDataSource =>
            val entitites = nestedSource.retrieveNested(nestedEntitySchema)
            NestedEntityTable(entitites, nestedEntitySchema, dataset)
          case _ =>
            throw TaskException(s"Dataset ${dataset.id.toString} is not capable of emitting nested entities! It can only output flat entities.")
        }

      case entitySchema: EntitySchema =>
        val entities = dataset.source.retrieve(entitySchema = schema)
        GenericEntityTable(entities, entitySchema = entitySchema, dataset)
    }
  }

  // read all triples from the data source
  private def readTriples(dataset: Task[Dataset], rdfDataset: RdfDataset) = {
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
  }

  override protected def write(data: EntityTable[EntityTrait, SchemaTrait], dataset: Task[Dataset]): Unit = {
    data match {
      case LinksTable(links, linkType, _) =>
        writeLinks(dataset, links, linkType)
      case TripleEntityTable(entities, _) =>
        writeTriples(dataset, entities)
      case NestedEntityTable(entities, nestedEntitySchema, _) =>
        // TODO
      case et: FlatEntityTable =>
        writeEntities(dataset, et)
    }
  }

  private def writeEntities(dataset: Dataset, entityTable: FlatEntityTable): Unit = {
    var entityCount = 0
    val startTime = System.currentTimeMillis()
    var lastLog = startTime
    val sink = dataset.entitySink
    sink.openWithTypedPath(entityTable.entitySchema.typedPaths)
    for (entity <- entityTable.entities) {
      val e = entity.asInstanceOf[Entity]
      sink.writeEntity(entity.uri, e.values)
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
}
