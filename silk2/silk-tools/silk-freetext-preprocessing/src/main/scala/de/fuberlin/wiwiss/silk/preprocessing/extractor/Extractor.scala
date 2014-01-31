package de.fuberlin.wiwiss.silk.preprocessing.extractor

import scala.xml.Node
import de.fuberlin.wiwiss.silk.preprocessing.entity.{Entity, Property}
import de.fuberlin.wiwiss.silk.preprocessing.transformer.Transformer
import de.fuberlin.wiwiss.silk.preprocessing.dataset.Dataset

/**
 * Created with IntelliJ IDEA.
 * User: Petar
 * Date: 21/01/14
 * Time: 14:07
 * To change this template use File | Settings | File Templates.
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

