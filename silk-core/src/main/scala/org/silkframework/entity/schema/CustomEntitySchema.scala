package org.silkframework.entity.schema

import org.silkframework.config.{SilkVocab, Task, TaskSpec}
import org.silkframework.entity.paths.{TypedPath, UntypedPath}
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

object ProjectFileEntitySchema extends CustomEntitySchema[Resource] {

  override val schema: EntitySchema = {
    EntitySchema(
      typeUri = Uri(SilkVocab.DatasetResourceSchemaType),
      typedPaths = IndexedSeq(
        TypedPath(UntypedPath(SilkVocab.resourcePath), ValueType.STRING, isAttribute = true)
      )
    )
  }

  override def toEntity(v: Resource)(implicit pluginContext: PluginContext): Entity = {
    //TODO this should use the relativePath method after it has been merged into develop
    val relativePath = v.path.stripPrefix(pluginContext.resources.basePath).stripPrefix("/").stripPrefix(File.separator)
    new Entity("", IndexedSeq(Seq(relativePath)), schema)
  }

  override def fromEntity(entity: Entity)(implicit pluginContext: PluginContext): Resource = {
    //TODO validate schema
    entity.values.head.headOption match {
      case Some(value) =>
        pluginContext.resources.getInPath(value)
      case None =>
        throw new ValidationException("No resource path provided")
    }
  }
}

//TODO delete temporary resources after use?
object LocalFileEntitySchema extends CustomEntitySchema[LocalFile] {

  override val schema: EntitySchema = {
    EntitySchema(
      typeUri = Uri(SilkVocab.DatasetResourceSchemaType),
      typedPaths = IndexedSeq(
        TypedPath(UntypedPath(SilkVocab.resourcePath), ValueType.STRING, isAttribute = true),
        TypedPath(UntypedPath(SilkVocab.contentType), ValueType.STRING, isAttribute = true)
      )
    )
  }

  override def toEntity(v: LocalFile)(implicit pluginContext: PluginContext): Entity = {
    new Entity("", IndexedSeq(Seq(v.file.path), v.contentType.toSeq), schema)
  }

  override def fromEntity(entity: Entity)(implicit pluginContext: PluginContext): LocalFile = {
    //TODO validate schema
    val file = entity.values.head.headOption match {
      case Some(value) =>
        FileResource(new File(value))
      case None =>
        throw new ValidationException("No resource path provided")
    }

    val contentType = entity.values(1).headOption.filter(_.isEmpty)

    LocalFile(file, contentType)
  }
}

case class LocalFile(file: FileResource, contentType: Option[String])
