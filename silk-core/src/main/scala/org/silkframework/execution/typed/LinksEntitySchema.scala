package org.silkframework.execution.typed
import org.silkframework.config.{SilkVocab, TaskSpec}
import org.silkframework.entity.paths.{TypedPath, UntypedPath}
import org.silkframework.entity.{Entity, EntitySchema, Link, LinkWithConfidence, ValueType}
import org.silkframework.execution.typed.TypedEntitiesVocab.{schemaPath, schemaType}
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.util.Uri

/**
 * Entity schema that holds a collection of links.
 */
object LinksEntitySchema extends TypedEntitySchema[Link, LinkGenerator] {

  /**
   * The fixed schema for this type.
   * Entities will be associated with this custom type based on the type URI of the schema.
   */
  override def schema: EntitySchema = {
    EntitySchema(
      typeUri = schemaType("Link"),
      typedPaths =
        IndexedSeq(
          TypedPath(schemaPath("linkTargetUri"), ValueType.URI, isAttribute = true),
          TypedPath(schemaPath("linkConfidence"), ValueType.DOUBLE, isAttribute = true)
        )
    )
  }

  /**
   * Creates a generic entity from a typed entity.
   */
  override def toEntity(link: Link)(implicit pluginContext: PluginContext): Entity = {
    Entity(
      uri = link.source,
      values = IndexedSeq(Seq(link.target), Seq(link.confidence.getOrElse(0.0).toString)),
      schema = schema
    )
  }

  /**
   * Creates a typed entity from a generic entity.
   */
  override def fromEntity(entity: Entity)(implicit pluginContext: PluginContext): Link = {
    new LinkWithConfidence(entity.uri, entity.values.head.head, entity.values(1).head.toDouble)
  }
}

/**
 * Base trait of all tasks that generate links.
 */
trait LinkGenerator extends TaskSpec {

  def linkType: Uri

  def inverseLinkType: Option[Uri]

}