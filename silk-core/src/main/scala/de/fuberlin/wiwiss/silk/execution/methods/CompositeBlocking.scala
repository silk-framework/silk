package de.fuberlin.wiwiss.silk.execution.methods

import de.fuberlin.wiwiss.silk.entity.{Index, Entity, Path}
import de.fuberlin.wiwiss.silk.rule.LinkageRule
import de.fuberlin.wiwiss.silk.execution.ExecutionMethod

/**
  * Blocking using a composite key built from two single keys.
  */
class CompositeBlocking(blockingKey1: Path, blockingKey2: Path) extends ExecutionMethod {

   override def indexEntity(entity: Entity, rule: LinkageRule): Index = {
     Index.oneDim(
       for(v1 <- entity.evaluate(blockingKey1);
           v2 <- entity.evaluate(blockingKey2)) yield {
         (v1 + v2).hashCode
       }
     )
   }

 }
