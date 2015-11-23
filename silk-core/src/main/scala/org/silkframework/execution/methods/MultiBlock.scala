package org.silkframework.execution.methods

import org.silkframework.entity.{Index, Entity}
import org.silkframework.rule.LinkageRule
import org.silkframework.execution.ExecutionMethod

/**
  * MultiBlock execution method.
  */
case class MultiBlock() extends ExecutionMethod {
  override def indexEntity(entity: Entity, rule: LinkageRule): Index = rule.index(entity, 0.0)
}
