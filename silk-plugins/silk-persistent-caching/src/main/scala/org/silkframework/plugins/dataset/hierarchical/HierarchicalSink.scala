package org.silkframework.plugins.dataset.hierarchical

import org.silkframework.config.Prefixes
import org.silkframework.dataset.{DirtyTrackingFileDataSink, EntitySink, TypedProperty}
import org.silkframework.entity.ValueType
import HierarchicalSink.{DEFAULT_MAX_SIZE, RDF_TYPE}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.WritableResource
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.Uri

import java.io.OutputStream
import scala.collection.mutable

/**
  * Sink that writes entities into a hierarchical output, such as JSON or XML.
  */
abstract class HierarchicalSink extends EntitySink {

  // Holds root entities
  private val rootEntities: SequentialEntityCache = SequentialEntityCache()

  // Holds nested entities
  private lazy val cache: HierarchicalEntityCache = HierarchicalEntityCache()

  // All properties for each table.
  private val tables: mutable.Buffer[TableSpec] = mutable.Buffer.empty

  // True, if there is at most a single root entity.
  private var singleRootEntity: Boolean = false

  // True, if a table is open at the moment.
  private var tableOpen: Boolean = false

  // Specifies whether attribute properties should be written before non-attribute properties
  protected val writeAttributesFirst: Boolean = false

  // Maximum depth of written hierarchical entities. This acts as a safe guard if a recursive structure is written.
  protected def maxDepth: Int = DEFAULT_MAX_SIZE

  /**
    * Writes the output by calling the supplied write function.
    * Must be implemented in sub classes.
    */
  protected def outputEntities(writeOutput: HierarchicalEntityWriter => Unit): Unit

  /**
   * Initializes this writer.
   *
   * @param properties The list of properties of the entities to be written.
   */
  override def openTable(typeUri: Uri, properties: Seq[TypedProperty], singleEntity: Boolean = false)
                        (implicit userContext: UserContext, prefixes: Prefixes): Unit = {
    if(this.tables.isEmpty) {
      singleRootEntity = singleEntity
    }
    this.tables.append(TableSpec.create(properties, writeAttributesFirst))
    tableOpen = true
  }

  /**
   * Writes a new entity.
   *
   * @param subjectURI The subject URI of the entity.
   * @param values  The list of values of the entity. For each property that has been provided
   *                when opening this writer, it must contain a set of values.
   */
  override def writeEntity(subjectURI: String, values: IndexedSeq[Seq[String]])
                          (implicit userContext: UserContext): Unit = {
    if(tables.size == 1) {
      // We are writing a root entity
      rootEntities.putEntity(subjectURI, values)
    } else {
      // We are writing a nested entity
      cache.putEntity(CachedEntity(subjectURI, values, tables.size - 1))
    }
  }

  override def closeTable()(implicit userContext: UserContext): Unit = {
    tableOpen = false
  }

  override def close()(implicit userContext: UserContext): Unit = {
    try {
      if(!tableOpen) { // only write if the current table has been closed regularly
        outputEntities(writeEntities)
      }
    } finally {
      cache.close()
      rootEntities.close()
    }
  }

  /**
   * Makes sure that the next write will start from an empty dataset.
   */
  override def clear()(implicit userContext: UserContext): Unit = { }

  /**
    * Outputs all entities in the cache to a HierarchicalEntityWriter.
    */
  private def writeEntities(writer: HierarchicalEntityWriter): Unit = {
    writer.open(singleRootEntity)
    rootEntities.readAndClose { entity =>
      outputEntity(entity, writer, 1)
    }
  }

  /**
    * Outputs a single entity and its referenced entities to the HierarchicalEntityWriter.
    * The entity is loaded from the cache by its URI.
    */
  private def outputEntityByUri(uri: String, writer: HierarchicalEntityWriter, depth: Int): Unit = {
    cache.getEntity(uri) match {
      case Some(entity) =>
        outputEntity(entity, writer, depth)
      case None =>
        throw new ValidationException("Could not find entity with URI: " + uri)
    }
  }

  /**
    * Outputs a single entity and its referenced entities to the HierarchicalEntityWriter.
    */
  private def outputEntity(entity: CachedEntity, writer: HierarchicalEntityWriter, depth: Int): Unit = {
    if(depth > math.min(maxDepth, tables.length)) {
      throw new MaxDepthExceededException("Exceeded maximum depth for writing entities. " +
        "This might happen if manual URI patterns are used that generate a recursive structure. Remove manual URI patterns in order to solve this." +
        "In case a large structure is to be written, increase the maxDepth parameter.")
    }

    writer.startEntity()
    val table = tables(entity.tableIndex)
    for(index <- table.propertyIndices) {
      val property = table.properties(index)
      val values = entity.values(index)
      writer.startProperty(property, values.size)
      if(property.valueType == ValueType.URI && property.propertyUri != RDF_TYPE) {
        for(v <- values) {
          outputEntityByUri(v, writer, depth + 1)
        }
      } else {
        writer.writeValue(values, property)
      }
      writer.endProperty(property)
    }
    writer.endEntity()
  }
}

object HierarchicalSink {

  final val DEFAULT_MAX_SIZE = 15

  final val RDF_TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type"

}




