package de.fuberlin.wiwiss.silk.execution.methods

import de.fuberlin.wiwiss.silk.entity.{Index, Entity}
import de.fuberlin.wiwiss.silk.rule.LinkageRule
import de.fuberlin.wiwiss.silk.execution.ExecutionMethod

/**
  * Full execution method.
  */
case class Full() extends ExecutionMethod {
   override def indexEntity(entity: Entity, rule: LinkageRule): Index = Index.default
 }
