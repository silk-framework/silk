package org.silkframework.preprocessing.extractor

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

