package de.fuberlin.wiwiss.silk.execution.methods

import de.fuberlin.wiwiss.silk.entity.{Index, Entity, Path}
import de.fuberlin.wiwiss.silk.linkagerule.LinkageRule
import de.fuberlin.wiwiss.silk.execution.ExecutionMethod
import de.fuberlin.wiwiss.silk.plugins.transformer.linguistic.SoundexTransformer
import de.fuberlin.wiwiss.silk.linkagerule.input.SimpleTransformer

/**
  * Traditional blocking.
  * Per-default, all values are soundex encoded before blocking.
  *
  * @param sourceKey The source blocking key, e.g., rdfs:label
  * @param targetKey The target blocking key, e.g., rdfs:label
  * @param q The maximum number of characters that are used for the indexing
 *  @param transformers A list of transformers that are applied to each value prior to blocking
  */
case class Blocking(sourceKey: Path, targetKey: Path, q: Int = 100, transformers: List[SimpleTransformer] = SoundexTransformer() :: Nil) extends ExecutionMethod {
   override def indexEntity(entity: Entity, rule: LinkageRule): Index = {
     val key = if(sourceKey.variable == entity.desc.variable) sourceKey else targetKey
     val values = entity.evaluate(key)

     Index.blocks(values.map(getBlock))
   }

   private def getBlock(value: String) = {
     val transformedValue = transformers.foldLeft(value)((value,transformer) => transformer.evaluate(value))
     transformedValue.take(q).hashCode
   }
}