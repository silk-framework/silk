package de.fuberlin.wiwiss.silk.execution.methods

import de.fuberlin.wiwiss.silk.entity.{Index, Entity, Path}
import de.fuberlin.wiwiss.silk.linkagerule.LinkageRule
import de.fuberlin.wiwiss.silk.execution.ExecutionMethod

/**
  * Traditional blocking.
  *
  * @param sourceKey The source blocking key, e.g., rdfs:label
  * @param targetKey The target blocking key, e.g., rdfs:label
  */
case class Blocking(sourceKey: Path, targetKey: Path) extends ExecutionMethod {

   override def indexEntity(entity: Entity, rule: LinkageRule): Index = {
     val key = if(sourceKey.variable == entity.desc.variable) sourceKey else targetKey
     val values = entity.evaluate(key)
     Index.blocks(values.map(_.hashCode))
   }

 }
