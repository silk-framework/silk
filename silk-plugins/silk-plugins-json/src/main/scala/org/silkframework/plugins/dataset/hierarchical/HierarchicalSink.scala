package org.silkframework.plugins.dataset.hierarchical

import org.silkframework.config.Prefixes
import org.silkframework.dataset.{EntitySink, TypedProperty}
import org.silkframework.entity.ValueType
import org.silkframework.plugins.dataset.hierarchical.HierarchicalSink.{DEFAULT_MAX_SIZE, RDF_TYPE}
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
  private lazy val cache: EntityCache = EntityCache()

  // All properties for each table.
  private val properties: mutable.Buffer[Seq[TypedProperty]] = mutable.Buffer.empty

  // True, if a table is open at the moment.
  private var tableOpen: Boolean = false

  // Maximum depth of written hierarchical entities. This acts as a safe guard if a recursive structure is written.
  protected def maxDepth: Int = DEFAULT_MAX_SIZE

  /**
    * The resource this sink is writing to.
    * Must be implemented in sub classes.
    */
  protected def resource: WritableResource

  /**
    * Creates a new HierarchicalEntityWriter instance that will be used to write the output.
    * Must be implemented in sub classes.
    */
  protected def createWriter(outputStream: OutputStream): HierarchicalEntityWriter

  /**
   * Initializes this writer.
   *
   * @param properties The list of properties of the entities to be written.
   */
  override def openTable(typeUri: Uri, properties: Seq[TypedProperty])
                        (implicit userContext: UserContext, prefixes: Prefixes): Unit = {
    this.properties.append(properties)
    tableOpen = true
  }

  /**
   * Writes a new entity.
   *
   * @param subjectURI The subject URI of the entity.
   * @param values  The list of values of the entity. For each property that has been provided
   *                when opening this writer, it must contain a set of values.
   */
  override def writeEntity(subjectURI: String, values: Seq[Seq[String]])
                          (implicit userContext: UserContext): Unit = {
    if(properties.size == 1) {
      rootEntities.putEntity(subjectURI, values)
    }
    cache.putEntity(CachedEntity(subjectURI, values, properties.size - 1))
  }

  override def closeTable()(implicit userContext: UserContext): Unit = {
    tableOpen = false
  }

  override def close()(implicit userContext: UserContext): Unit = {
    try {
      if(!tableOpen) { // only write if the current table has been closed regularly
        resource.write() { outputStream =>
          val writer = createWriter(outputStream)
          try {
            outputEntities(writer)
          } finally {
            writer.close()
          }
        }
      }
    } finally {
      cache.close()
    }
  }

  /**
   * Makes sure that the next write will start from an empty dataset.
   */
  override def clear()(implicit userContext: UserContext): Unit = {
    resource.delete()
  }

  /**
    * Outputs all entities in the cache to a HierarchicalEntityWriter.
    */
  private def outputEntities(writer: HierarchicalEntityWriter): Unit = {
    writer.open()
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
    if(depth > math.min(maxDepth, properties.length)) {
      throw new MaxDepthExceededException("Exceeded maximum depth for writing entities. " +
        "This might happen if manual URI patterns are used that generate a recursive structure. Remove manual URI patterns in order to solve this." +
        "In case a large structure is to be written, increase the maxDepth parameter.")
    }

    writer.startEntity()
    for((value, property) <- entity.values zip properties(entity.tableIndex)) {
      writer.startProperty(property, value.size)
      if(property.valueType == ValueType.URI && property.propertyUri != RDF_TYPE) {
        for(v <- value) {
          outputEntityByUri(v, writer, depth + 1)
        }
      } else {
        writer.writeValue(value, property)
      }
      writer.endProperty(property)
    }
    writer.endEntity()
  }
}

object HierarchicalSink {

  final val DEFAULT_MAX_SIZE = 15

  private val RDF_TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type"

}




