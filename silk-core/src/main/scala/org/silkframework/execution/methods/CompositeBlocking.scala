package org.silkframework.execution.methods

import org.silkframework.entity.{Index, Entity, Path}
import org.silkframework.rule.LinkageRule
import org.silkframework.execution.ExecutionMethod

/**
  * Blocking using a composite key built from two single keys.
  */
class CompositeBlocking(blockingKey1: Path, blockingKey2: Path) extends ExecutionMethod {

   override def indexEntity(entity: Entity, rule: LinkageRule, sourceOrTarget: Boolean): Index = {
     val blocks =
       for(v1 <- entity.evaluate(blockingKey1);
           v2 <- entity.evaluate(blockingKey2)) yield {
         (v1 + v2).hashCode
       }
     Index.oneDim(blocks.toSet)
   }

 }
