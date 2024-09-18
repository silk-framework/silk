package org.silkframework.entity.schema

import org.silkframework.config.{SilkVocab, Task, TaskSpec}
import org.silkframework.entity.paths.{TypedPath, UntypedPath}
import org.silkframework.entity.schema.FileType.FileType
import org.silkframework.entity.{Entity, EntitySchema, ValueType}
import org.silkframework.execution.local.LocalEntities
import org.silkframework.runtime.iterator.CloseableIterator
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.resource.{FileResource, Resource}
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.Uri

import java.io.File

//TODO move this to another package as LocalEntities is also in execution?

abstract class CustomEntitySchema[EntityType] {

  def schema: EntitySchema

  def toEntity(v: EntityType)(implicit pluginContext: PluginContext): Entity

  def fromEntity(entity: Entity)(implicit pluginContext: PluginContext): EntityType

  def unapply(entities: LocalEntities)(implicit pluginContext: PluginContext): Option[CustomEntities[EntityType]] = {
    //TODO if(entities.isInstanceOf[CustomEntities])
    if(entities.entitySchema.typeUri == schema.typeUri) {
      Some(new CustomEntities[EntityType](
        customEntities = entities.entities.map(fromEntity),
        customEntitySchema = this,
        task = entities.task
      ))
    } else {
      None
    }
  }

  def create(values: CloseableIterator[EntityType], task: Task[TaskSpec])
            (implicit pluginContext: PluginContext): CustomEntities[EntityType] = {
    new CustomEntities(values, this, task)
  }
}

class CustomEntities[EntityType](val customEntities: CloseableIterator[EntityType],
                                 val customEntitySchema: CustomEntitySchema[EntityType],
                                 override val task: Task[TaskSpec])
                                (implicit pluginContext: PluginContext) extends LocalEntities {

  /**
   * The schema of the entities
   */
  override def entitySchema: EntitySchema = customEntitySchema.schema

  /**
   * The entities in this table.
   */
  override def entities: CloseableIterator[Entity] = {
    customEntities.map(customEntitySchema.toEntity)
  }

  override def updateEntities(newEntities: CloseableIterator[Entity], newSchema: EntitySchema): LocalEntities = {
    //TODO implement correctly
    this // Changing entities has no effect
  }

}

//TODO delete temporary resources after use?
object FileEntitySchema extends CustomEntitySchema[FileEntity] {

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
    val fileType = FileType.Local //TODO entity.values(1).headOption.map(FileType.values.byName.apply).getOrElse(FileType.Local)
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
}

case class FileEntity(file: Resource, fileType: FileType, contentType: Option[String] = None)

object FileType extends Enumeration {
  type FileType = Value
  val Project, Local = Value
}
