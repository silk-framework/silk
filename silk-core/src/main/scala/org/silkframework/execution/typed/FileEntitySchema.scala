package org.silkframework.execution.typed

import org.silkframework.config.{SilkVocab, Task, TaskSpec}
import org.silkframework.entity.paths.{TypedPath, UntypedPath}
import org.silkframework.entity.{Entity, EntitySchema, ValueType}
import org.silkframework.execution.typed.FileType.FileType
import org.silkframework.execution.typed.TypedEntitiesVocab.{schemaPath, schemaType}
import org.silkframework.runtime.iterator.CloseableIterator
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.resource.{FileResource, WritableResource}
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.Uri

import java.io.File

/**
 * Entity schema that holds a collection of files.
 */
object FileEntitySchema extends TypedEntitySchema[FileEntity, TaskSpec] {

  /**
   * The fixed schema for file entities.
   * Entities will be associated with this custom type based on the type URI of the schema, i.e., the type URI must be unique.
   */
  override val schema: EntitySchema = {
    EntitySchema(
      typeUri = schemaType("File"),
      typedPaths = IndexedSeq(
        TypedPath(schemaPath("path"), ValueType.STRING, isAttribute = true),
        TypedPath(schemaPath("fileType"), ValueType.STRING, isAttribute = true),
        TypedPath(schemaPath("mimeType"), ValueType.STRING, isAttribute = true),
      )
    )
  }

  /**
   * Creates a generic entity from a file.
   */
  override def toEntity(entity: FileEntity)(implicit pluginContext: PluginContext): Entity = {
    val path = entity.fileType match {
      case FileType.Project =>
        //TODO this should use the relativePath method after it has been merged into develop
        entity.file.path.stripPrefix(pluginContext.resources.basePath).stripPrefix("/").stripPrefix(File.separator)
      case FileType.Local =>
        entity.file.path
    }
    new Entity(new File(entity.file.path).toURI.toString, IndexedSeq(Seq(path), Seq(entity.fileType.toString), entity.mimeType.toSeq), schema)
  }

  /**
   * Parses a file from a generic entity.
   * Implementations assumes that the incoming schema matches the schema expected by this typed schema, i.e., schema validation is not done.
   */
  override def fromEntity(entity: Entity)(implicit pluginContext: PluginContext): FileEntity = {
    val fileType = entity.values(1).headOption.map(FileType.withName).getOrElse(FileType.Local)
    val contentType = entity.values(2).headOption.filter(_.nonEmpty)

    val file = entity.values(0).headOption match {
      case Some(value) =>
        fileType match {
          case FileType.Project =>
            pluginContext.resources.getInPath(value)
          case FileType.Local =>
            FileResource(new File(value))
        }
      case None =>
        throw new ValidationException("No resource path provided")
    }

    FileEntity(file, fileType, contentType)
  }

  /**
   * Creates a single local file entity.
   */
  def local(resource: FileResource, task: Task[TaskSpec])(implicit pluginContext: PluginContext): TypedEntities[FileEntity, TaskSpec] = {
    create(CloseableIterator.single(FileEntity(resource, FileType.Local)), task)
  }
}
/**
 * A file entity that can be held in a `FileEntitySchema`.
 *
 * @param file The file to be exchanged.
 * @param fileType The file type. Affects how the file will be serialized to a generic entity table.
 * @param mimeType Optional content type.
 */
case class FileEntity(file: WritableResource, fileType: FileType, mimeType: Option[String] = None)

object FileEntity {

  /**
   * Creates a temporary file entity.
   * File will be deleted on garbage collection of the returned entity and latest on exit.
   */
  def createTemp(prefix: String, suffix: String = ".tmp"): FileEntity = {
    val tempFile = File.createTempFile(prefix, suffix)
    tempFile.deleteOnExit()
    val tempResource = FileResource(tempFile)
    tempResource.setDeleteOnGC(true) // Get rid of temporary file when file resource is garbage collected
    FileEntity(tempResource, FileType.Local)
  }

}

/**
 * Determines where a `FileEntity` is held.
 * One of:
 * - Project: Files that are part of the project resources. Identified by the relative path within the project resources.
 * - Local: File on the local file system. Identified by the full local path.
 */
object FileType extends Enumeration {
  type FileType = Value
  val Project, Local = Value
}
