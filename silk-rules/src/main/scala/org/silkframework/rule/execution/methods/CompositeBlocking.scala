package org.silkframework.rule.execution.methods

import org.silkframework.entity.paths.UntypedPath
import org.silkframework.entity.{Entity, Index}
import org.silkframework.rule.execution.ExecutionMethod
import org.silkframework.rule.LinkageRule

/**
  * Blocking using a composite key built from two single keys.
  */
class CompositeBlocking(blockingKey1: UntypedPath, blockingKey2: UntypedPath) extends ExecutionMethod {

   override def indexEntity(entity: Entity, rule: LinkageRule, sourceOrTarget: Boolean): Index = {
     val blocks =
       for(v1 <- entity.evaluate(blockingKey1);
           v2 <- entity.evaluate(blockingKey2)) yield {
         (v1 + v2).hashCode
       }
     Index.oneDim(blocks.toSet)
   }

 }
