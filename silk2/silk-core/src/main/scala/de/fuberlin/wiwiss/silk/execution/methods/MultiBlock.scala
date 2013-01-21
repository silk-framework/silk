package de.fuberlin.wiwiss.silk.execution.methods

import de.fuberlin.wiwiss.silk.entity.{Index, Entity}
import de.fuberlin.wiwiss.silk.linkagerule.LinkageRule
import de.fuberlin.wiwiss.silk.execution.ExecutionMethod

/**
  * MultiBlock execution method.
  */
class MultiBlock extends ExecutionMethod {
   def indexEntity(entity: Entity, rule: LinkageRule): Index = rule.index(entity, 0.0)
 }
