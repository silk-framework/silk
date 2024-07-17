package org.silkframework.execution.report

import org.silkframework.entity.Entity

/** A sampled entity for an execution report.
  *
  * @param uri The URI of the entity.
  * @param values The input/output values of the entity.
  */
case class EntitySample(uri: String, values: IndexedSeq[Seq[String]])

object EntitySample {
  def entityToEntitySample(entity: Entity): EntitySample = {
    EntitySample(entity.uri, entity.values)
  }
}