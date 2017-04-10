package org.silkframework.entity

import scala.language.implicitConversions

/**
  * Base trait to define an input or output schema.
  */
trait SchemaTrait

object SchemaTrait {
  implicit def toEntitySchema(schemaTrait: SchemaTrait): EntitySchema = {
    schemaTrait match {
      case entitySchema: EntitySchema =>
        entitySchema
      case hierarchicalSchema: HierarchicalSchema =>
        hierarchicalSchema
    }
  }

  implicit def toHierarchicalSchema(schemaTrait: SchemaTrait): HierarchicalSchema = {
    schemaTrait match {
      case entitySchema: EntitySchema =>
        entitySchema
      case hierarchicalSchema: HierarchicalSchema =>
        hierarchicalSchema
    }
  }
}