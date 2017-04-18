package org.silkframework.entity

/**
  * A nested entity. The structure should correspond to the structure of the [[NestedEntitySchema]].
  */
case class NestedEntity(uri: String,
                        values: IndexedSeq[Seq[String]],
                        nestedEntities: IndexedSeq[Seq[NestedEntity]]) extends EntityTrait