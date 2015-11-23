package org.silkframework.execution.methods

import org.silkframework.entity.{Index, Entity, Path}
import org.silkframework.rule.LinkageRule
import org.silkframework.execution.ExecutionMethod

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
