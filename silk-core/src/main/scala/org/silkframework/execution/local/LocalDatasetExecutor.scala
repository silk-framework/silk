package org.silkframework.execution.local

import java.util.logging.{Level, Logger}

import org.silkframework.dataset.rdf.RdfDataset
import org.silkframework.dataset.{Dataset, TripleSinkDataset}
import org.silkframework.entity.{Entity, EntitySchema}
import org.silkframework.execution.{DatasetExecutor, TaskException}

/**
  * Created on 7/20/16.
  */
class LocalDatasetExecutor extends DatasetExecutor[Dataset, LocalExecution] {
  private val logger = Logger.getLogger(getClass.getName)

  /**
    * Reads data from a dataset.
    */
  override def read(dataset: Dataset, schema: EntitySchema): EntityTable = {
    schema match {
      case TripleEntitySchema.schema =>
        dataset match {
          case rdfDataset: RdfDataset =>
            val sparqlResult = rdfDataset.sparqlEndpoint.select("SELECT ?s ?p ?o WHERE {?s ?p ?o}")
            val tripleEntities = sparqlResult.bindings.view map { resultMap =>
              val s = resultMap("s").value
              val p = resultMap("p").value
              val o = resultMap("o").value
              new Entity(s, IndexedSeq(Seq(s), Seq(p), Seq(o)), TripleEntitySchema.schema)
            }
            TripleEntityTable(tripleEntities)
          case _ =>
            throw new TaskException("Dataset is not a RDF dataset and thus cannot output triples!")
        }
      case _ =>
        val entities = dataset.source.retrieve(entitySchema = schema)
        GenericEntityTable(entities, entitySchema = schema)
    }
  }

  override protected def write(data: EntityTable, dataset: Dataset): Unit = {
    var entityCount = 0
    val startTime = System.currentTimeMillis()
    var lastLog = startTime
    data match {
      case LinksTable(links, linkType) =>
        val sink = dataset.linkSink
        sink.writeLinks(links, linkType.uri)
        val time = (System.currentTimeMillis - startTime) / 1000.0
        logger.log(Level.INFO, "Finished writing " + entityCount + " links in " + time + " seconds")
      case TripleEntityTable(entities) =>
        dataset match {
          case rdfDataset: TripleSinkDataset =>
            writeTriples(entities, rdfDataset)
          case _ =>
            throw new TaskException("Cannot write triples to non-RDF dataset!")
        }
      case et: EntityTable =>
        val sink = dataset.entitySink
        sink.open(et.entitySchema.paths.map(_.propertyUri.get.toString))
        for (entity <- et.entities) {
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
        logger.log(Level.INFO, "Finished writing " + entityCount + " entities with type '" + data.entitySchema.typeUri + "' in " + time + " seconds")
    }
  }

  private def writeTriples(entities: Traversable[Entity], tripleSinkDataset: TripleSinkDataset): Unit = {
    val sink = tripleSinkDataset.tripleSink
    sink.init()
    for (entity <- entities) {
      if (entity.values.size != 3) {
        throw new scala.RuntimeException("Did not get exactly 3 values for triple entity!")
      }
      try {
        val Seq(s, p, o) = entity.values.map(_.head)
        sink.writeTriple(s, p, o)
      } catch {
        case e: Exception =>
          throw new scala.RuntimeException("Triple entity with empty values")
      }
    }
    sink.close()
  }

  protected def emptyResult: GenericEntityTable = {
    GenericEntityTable(Seq(), entitySchema = EntitySchema.empty)
  }
}
