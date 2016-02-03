package org.silkframework.execution.methods

import org.silkframework.entity.{Index, Entity}
import org.silkframework.rule.LinkageRule
import org.silkframework.execution.ExecutionMethod

/**
  * Full execution method.
  */
case class Full() extends ExecutionMethod {
   override def indexEntity(entity: Entity, rule: LinkageRule, sourceOrTarget: Boolean): Index = Index.default
 }
