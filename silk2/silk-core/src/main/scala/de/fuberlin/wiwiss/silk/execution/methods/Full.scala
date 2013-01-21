package de.fuberlin.wiwiss.silk.execution.methods

import de.fuberlin.wiwiss.silk.entity.{Index, Entity}
import de.fuberlin.wiwiss.silk.linkagerule.LinkageRule
import de.fuberlin.wiwiss.silk.execution.ExecutionMethod

/**
  * Full execution method.
  */
class Full extends ExecutionMethod {
   def indexEntity(entity: Entity, rule: LinkageRule): Index = Index.default
 }
