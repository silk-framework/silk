package org.silkframework.preprocessing.extractor

import scala.xml.Node
import org.silkframework.preprocessing.entity.{Entity, Property}
import org.silkframework.preprocessing.transformer.Transformer
import org.silkframework.preprocessing.dataset.Dataset

/**
 * Represents an extractor.
 *
 */
trait Extractor{

  val id:String
  val propertyToExtractFrom: String
  val transformers:List[Transformer]

  def apply(dataset:Dataset, findNewProperty: String => String):Traversable[Entity]

  def applyTransformation(values:List[String]) = {
    def applyTransformationAcc(transformers:List[Transformer], values:List[String]):Traversable[String] = transformers match {
      case Nil => values
      case transformer::Nil => transformer.apply(values)
      case transformer::rest => applyTransformationAcc(rest, transformer.apply(values))
    }

    applyTransformationAcc(transformers, values)
  }

}

abstract class AutoExtractor extends Extractor{
    val propertyForTraining: String
    def train(dataset:Dataset)
}

abstract class ManualExtractor extends Extractor{
  val param: String
}

