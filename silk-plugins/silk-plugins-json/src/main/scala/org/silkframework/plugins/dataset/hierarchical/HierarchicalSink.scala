package org.silkframework.plugins.dataset.hierarchical

import org.silkframework.config.Prefixes
import org.silkframework.dataset.{EntitySink, TypedProperty}
import org.silkframework.entity.ValueType
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

  private lazy val cache: EntityCache = EntityCache()

  private val properties: mutable.Buffer[Seq[TypedProperty]] = mutable.Buffer.empty

  private val rootEntities: mutable.Buffer[String] = mutable.Buffer.empty

  private var tableOpen: Boolean = false

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
      rootEntities.append(subjectURI)
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
            writeEntities(writer)
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

  private def writeEntities(writer: HierarchicalEntityWriter): Unit = {
    writer.open()
    for (entityUri <- rootEntities) {
      writeEntity(entityUri, writer)
    }
  }

  private def writeEntity(uri: String, writer: HierarchicalEntityWriter): Unit = {
    cache.getEntity(uri) match {
      case Some(entity) =>
        writeEntity(entity, writer)
      case None =>
        throw new ValidationException("Could not find entity with URI: " + uri)
    }
  }

  private def writeEntity(entity: CachedEntity, writer: HierarchicalEntityWriter): Unit = {
    writer.startEntity()
    for((value, property) <- entity.values zip properties(entity.tableIndex)) {
      writer.startProperty(property, value.size)
      if(property.valueType == ValueType.URI) {
        for(v <- value) {
          writeEntity(v, writer)
        }
      } else {
        writer.writeValue(value, property)
      }
      writer.endProperty(property)
    }
    writer.endEntity()
  }
}




