package org.silkframework.dataset

import org.silkframework.entity.{NestedEntity, NestedSchemaNode}

/**
  * Trait that defines the API for the nested entity sink.
  */
trait NestedEntitySink {
  /** Writes a nested entity at the "current" place in the entity sink */
  def writeNestedEntity(nestedEntity: NestedEntity, nestedEntitySchema: NestedSchemaNode): Unit
}
