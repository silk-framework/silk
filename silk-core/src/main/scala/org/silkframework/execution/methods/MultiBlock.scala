package org.silkframework.execution.methods

import org.silkframework.entity.{Entity, Index}
import org.silkframework.execution.ExecutionMethod
import org.silkframework.rule.LinkageRule

/**
  * MultiBlock execution method.
  */
case class MultiBlock() extends ExecutionMethod {
  override def indexEntity(entity: Entity, rule: LinkageRule, sourceOrTarget: Boolean): Index = rule.index(entity, sourceOrTarget, 0.0)
}
