package org.silkframework.entity

/**
  * A nested entity
  */
case class NestedEntity(uri: String,
                        values: IndexedSeq[Seq[String]],
                        nestedEntities: IndexedSeq[NestedEntity]) extends EntityTrait