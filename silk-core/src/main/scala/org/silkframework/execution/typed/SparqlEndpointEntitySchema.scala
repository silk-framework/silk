package org.silkframework.execution.typed

import org.silkframework.dataset.DatasetSpec
import org.silkframework.dataset.rdf.RdfDataset
import org.silkframework.entity.{Entity, EntitySchema}
import org.silkframework.execution.typed.TypedEntitiesVocab.schemaType
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.util.Uri

object SparqlEndpointEntitySchema extends TypedEntitySchema[Unit, DatasetSpec[RdfDataset]] {

  /**
   * The fixed schema for this type.
   * Entities will be associated with this custom type based on the type URI of the schema.
   */
  override val schema: EntitySchema = {
    EntitySchema(
      typeUri = Uri(schemaType("SparqlEndpoint")),
      typedPaths = IndexedSeq.empty
    )
  }

  /**
   * Creates a generic entity from a typed entity.
   */
  override def toEntity(entity: Unit)(implicit pluginContext: PluginContext): Entity = {
    Entity.empty("")
  }

  /**
   * Creates a typed entity from a generic entity.
   */
  override def fromEntity(entity: Entity)(implicit pluginContext: PluginContext): Unit = {
    ()
  }
}
