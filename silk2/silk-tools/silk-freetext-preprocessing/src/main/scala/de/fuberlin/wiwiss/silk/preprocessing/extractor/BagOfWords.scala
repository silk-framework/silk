package de.fuberlin.wiwiss.silk.preprocessing.extractor

import de.fuberlin.wiwiss.silk.preprocessing.transformer.Transformer
import de.fuberlin.wiwiss.silk.preprocessing.entity.{Property, Entity}
import de.fuberlin.wiwiss.silk.preprocessing.dataset.Dataset
import scala.collection.mutable.{ArrayBuffer,HashMap}

/**
 * Created with IntelliJ IDEA.
 * User: Petar
 * Date: 25/12/13
 * Time: 15:18
 * To change this template use File | Settings | File Templates.
 */
class BagOfWords(override val id:String,
                 override val propertyToExtractFrom:String,
                 override val transformers:List[Transformer],
                 override val propertyForTraining:String) extends AutoExtractor{


    var map = new HashMap[String,ArrayBuffer[String]]

    override def train(dataset:Dataset)={

        val filteredEntities = dataset.filter(propertyForTraining)

        for(entity <- filteredEntities){
          for(property <- entity.properties){
            if(map.isEmpty){
              map += property.path -> ArrayBuffer.empty[String]
              map(property.path) = map(property.path) ++ property.value.toLowerCase.split("\\s")
            }
            else{
              if(map.contains(property.path)){
                if(!map(property.path).contains(property.value))
                  map(property.path) = map(property.path) ++ property.value.toLowerCase.split("\\s")
              }
              else{
                map += property.path -> ArrayBuffer.empty[String]
                map(property.path) = map(property.path) ++ property.value.toLowerCase.split("\\s")
              }
            }
          }
        }
    }

    def checkModel(value: String): Boolean = {
      var bool = false

      for(key <- map.keySet){
        if(map(key).contains(value.toLowerCase)) bool = true
      }
      bool
    }

    def applyTransformation(values:List[String]) = {
      def applyTransformationAcc(transformers:List[Transformer], values:List[String]):Traversable[String] = transformers match {
        case Nil => Traversable.empty[String]
        case transformer::Nil => transformer.apply(values)
        case transformer::rest => applyTransformationAcc(rest, transformer.apply(values))
      }

      applyTransformationAcc(transformers, values)
    }

    def apply(dataset:Dataset):Traversable[Entity]= {

      val filteredEntities = dataset.filter(propertyToExtractFrom)

      for(entity <- filteredEntities) yield {
        val extractedProperties = for(property <- entity.properties) yield{
          val values = applyTransformation(List(property.value)).filter(value => checkModel(value)).toList.distinct
          new Property(propertyForTraining, values.headOption.getOrElse(""))
        }
        new Entity(entity.uri, extractedProperties)
      }
    }
}
