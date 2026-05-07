package org.silkframework.rule.execution.methods

import org.silkframework.entity.{Entity, Index}
import org.silkframework.rule.execution.ExecutionMethod
import org.silkframework.rule.LinkageRuleExecution

/**
  * Full execution method.
  */
case class Full() extends ExecutionMethod {
   override def indexEntity(entity: Entity, rule: LinkageRuleExecution, sourceOrTarget: Boolean): Index = Index.default
 }
