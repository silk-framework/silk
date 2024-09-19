package org.silkframework.execution.typed

import org.silkframework.config.{SilkVocab, TaskSpec}
import org.silkframework.entity.paths.{TypedPath, UntypedPath}
import org.silkframework.entity.{Entity, EntitySchema, ValueType}
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.util.Uri

import java.util.UUID

/** Entity table that holds SPARQL Update queries */
object SparqlUpdateEntitySchema extends TypedEntitySchema[String, TaskSpec] {

  /**
   * The fixed schema for this type.
   * Entities will be associated with this custom type based on the type URI of the schema, i.e., the type URI must be unique.
   */
  override def schema: EntitySchema = {
    EntitySchema(
      typeUri = Uri(SilkVocab.SparqlUpdateSchemaType),
      typedPaths = IndexedSeq(
        TypedPath(UntypedPath(SilkVocab.sparqlUpdateQuery), ValueType.STRING, isAttribute = true)
      )
    )
  }

  /**
   * Creates a generic entity from a typed entity.
   */
  override def toEntity(query: String)(implicit pluginContext: PluginContext): Entity = {
    new Entity("urn:uuid:" + UUID.fromString(query), IndexedSeq(Seq(query)), schema)
  }

  /**
   * Creates a typed entity from a generic entity.
   */
  override def fromEntity(entity: Entity)(implicit pluginContext: PluginContext): String = {
    entity.values.head.headOption.getOrElse(throw new IllegalArgumentException("Query missing"))
  }
}
