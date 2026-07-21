package org.silkframework.execution.report

import org.silkframework.config.Prefixes
import org.silkframework.entity.{Entity, EntitySchema}

/** Sample entities for the execution report.
  *
  * @param entities The collected sample entities.
  * @param schema   The schema associated with the entities.
  * @param id       An optional task specific ID. E.g. that better matches in case of multiple entity tables.
  */
case class SampleEntities(entities: Seq[EntitySample],
                          schema: SampleEntitiesSchema,
                          id: Option[String] = None) {

  /** A copy with each sample value truncated to at most `maxValueChars` characters. */
  def truncateValues(maxValueChars: Int): SampleEntities = {
    copy(entities = entities.map(_.truncateValues(maxValueChars)))
  }
}

/** Simplified entity schema for the execution report. */
case class SampleEntitiesSchema(typeUri: String, typePath: String, properties: IndexedSeq[String])

/** A sampled entity for an execution report.
  *
  * @param uri The URI of the entity.
  * @param values The input/output values of the entity.
  */
case class EntitySample private(uri: String, values: IndexedSeq[Seq[String]]) {

  /** A copy with each value truncated to at most `maxValueChars` characters. */
  def truncateValues(maxValueChars: Int): EntitySample = {
    new EntitySample(uri, values.map(_.map(EntitySample.truncate(_, maxValueChars))))
  }
}

object EntitySample {
  // Max value char size to prevent from storing too large strings
  final val maxValueCharSize: Int = 1000

  def apply(value: String): EntitySample = new EntitySample("", IndexedSeq(Seq(value)))

  /** Does post-processing to the values, e.g. truncating them if they are too long. */
  def apply(uri: String, values: IndexedSeq[Seq[String]]): EntitySample = {
    new EntitySample(uri, values.map(_.map(truncate(_, maxValueCharSize))))
  }

  private def truncate(value: String, maxChars: Int): String = {
    if(value.length <= maxChars) value else value.substring(0, maxChars) + "…"
  }

  def entityToEntitySample(entity: Entity): EntitySample = {
    EntitySample(entity.uri, entity.values)
  }
}

object SampleEntitiesSchema {
  def entitySchemaToSampleEntitiesSchema(entitySchema: EntitySchema)
                                        (implicit prefixes: Prefixes): SampleEntitiesSchema = {
    SampleEntitiesSchema(
      typeUri = entitySchema.typeUri.serialize,
      typePath = entitySchema.subPath.serialize(),
      properties = entitySchema.typedPaths.map(_.serialize())
    )
  }

  final val empty = SampleEntitiesSchema("", "", IndexedSeq.empty)
}