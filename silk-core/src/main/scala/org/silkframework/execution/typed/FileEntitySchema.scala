package org.silkframework.execution.typed

import org.silkframework.config.{SilkVocab, Task, TaskSpec}
import org.silkframework.entity.paths.{TypedPath, UntypedPath}
import org.silkframework.entity.{Entity, EntitySchema, ValueType}
import org.silkframework.execution.typed.FileType.FileType
import org.silkframework.runtime.iterator.CloseableIterator
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.resource.{FileResource, WritableResource}
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.Uri

import java.io.File

/**
 * Entity schema that holds a collection of files.
 */
object FileEntitySchema extends TypedEntitySchema[FileEntity] {

  override val schema: EntitySchema = {
    EntitySchema(
      typeUri = Uri(SilkVocab.DatasetResourceSchemaType),
      typedPaths = IndexedSeq(
        TypedPath(UntypedPath(SilkVocab.resourcePath), ValueType.STRING, isAttribute = true),
        TypedPath(UntypedPath(SilkVocab.fileType), ValueType.STRING, isAttribute = true),
        TypedPath(UntypedPath(SilkVocab.contentType), ValueType.STRING, isAttribute = true),
      )
    )
  }

  override def toEntity(v: FileEntity)(implicit pluginContext: PluginContext): Entity = {
    val path = v.fileType match {
      case FileType.Project =>
        //TODO this should use the relativePath method after it has been merged into develop
        v.file.path.stripPrefix(pluginContext.resources.basePath).stripPrefix("/").stripPrefix(File.separator)
      case FileType.Local =>
        v.file.path
    }
    new Entity(new File(v.file.path).toURI.toString, IndexedSeq(Seq(path), Seq(v.fileType.toString), v.contentType.toSeq), schema)
  }

  override def fromEntity(entity: Entity)(implicit pluginContext: PluginContext): FileEntity = {
    //TODO validate schema
    val fileType = entity.values(1).headOption.map(FileType.withName).getOrElse(FileType.Local)
    val contentType = entity.values(2).headOption.filter(_.isEmpty)

    val file = entity.values.head.headOption match {
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

  def local(resource: FileResource, task: Task[TaskSpec])(implicit pluginContext: PluginContext): TypedEntities[FileEntity] = {
    create(CloseableIterator.single(FileEntity(resource, FileType.Local)), task)
  }
}
/**
 * A file entity that can be held in a `FileEntitySchema`.
 *
 * @param file The file to be exchanged.
 * @param fileType The file type. Affects how the file will be serialized to a generic entity table.
 * @param contentType Optional content type.
 */
case class FileEntity(file: WritableResource, fileType: FileType, contentType: Option[String] = None)

object FileEntity {

  /**
   * Creates a temporary file entity.
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
