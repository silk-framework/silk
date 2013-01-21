package de.fuberlin.wiwiss.silk.execution.methods

import de.fuberlin.wiwiss.silk.entity.{Index, Entity, Path}
import de.fuberlin.wiwiss.silk.linkagerule.LinkageRule
import de.fuberlin.wiwiss.silk.execution.ExecutionMethod

/**
  * Traditional blocking.
  *
  * @param blockingKey The blocking key, e.g., rdfs:label
  */
class Blocking(blockingKey: Path) extends ExecutionMethod {

   override def indexEntity(entity: Entity, rule: LinkageRule): Index = {
     val values = entity.evaluate(blockingKey)
     Index.oneDim(values.map(_.hashCode))
   }

 }
