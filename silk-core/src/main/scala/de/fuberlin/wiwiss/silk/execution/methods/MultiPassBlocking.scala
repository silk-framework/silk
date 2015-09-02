package de.fuberlin.wiwiss.silk.execution.methods

import de.fuberlin.wiwiss.silk.entity.{Index, Entity, Path}
import de.fuberlin.wiwiss.silk.rule.LinkageRule
import de.fuberlin.wiwiss.silk.execution.ExecutionMethod

/**
  * Multi-pass blocking.
  *
  * @param blockingKeys The blocking keys.
  */
class MultiPassBlocking(blockingKeys: Set[Path]) extends ExecutionMethod {

   override def indexEntity(entity: Entity, rule: LinkageRule): Index = {
     val values = blockingKeys.flatMap(key => entity.evaluate(key))
     Index.oneDim(values.map(_.hashCode))
   }

 }
