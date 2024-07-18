package org.silkframework.execution.report

import org.silkframework.config.Prefixes
import org.silkframework.entity.{Entity, EntitySchema}

/** Sample entities for the execution report. */
case class SampleEntities(entities: Seq[EntitySample], schema: Option[SampleEntitiesSchema])
/** Simplified entity schema for the execution report. */
case class SampleEntitiesSchema(typeUri: String, typePath: String, properties: IndexedSeq[String])

/** A sampled entity for an execution report.
  *
  * @param uri The URI of the entity.
  * @param values The input/output values of the entity.
  */
case class EntitySample(uri: String, values: IndexedSeq[Seq[String]])

object EntitySample {
  def apply(value: String): EntitySample = EntitySample("", IndexedSeq(Seq(value)))

  def entityToEntitySample(entity: Entity): EntitySample = {
    EntitySample(entity.uri, entity.values)
  }
}

object SampleEntitiesSchema {
  def entitySchemaToSampleEntitiesSchema(entitySchema: EntitySchema)
                                        (implicit prefixes: Prefixes): SampleEntitiesSchema = {
    SampleEntitiesSchema(
      typeUri = entitySchema.typeUri.uri,
      typePath = entitySchema.subPath.serialize(),
      properties = entitySchema.typedPaths.map(_.serialize())
    )
  }
}