package org.silkframework.entity

import scala.language.implicitConversions

/**
  * Base trait to define an input or output schema.
  */
trait SchemaTrait {
  type EntityType <: EntityTrait
}

object SchemaTrait {
  implicit def toEntitySchema(schemaTrait: SchemaTrait): EntitySchema = {
    schemaTrait match {
      case entitySchema: EntitySchema =>
        entitySchema
      case nestedSchema: NestedEntitySchema =>
        nestedSchema
    }
  }

  implicit def toNestedSchema(schemaTrait: SchemaTrait): NestedEntitySchema = {
    schemaTrait match {
      case entitySchema: EntitySchema =>
        entitySchema
      case nestedSchema: NestedEntitySchema =>
        nestedSchema
    }
  }
}