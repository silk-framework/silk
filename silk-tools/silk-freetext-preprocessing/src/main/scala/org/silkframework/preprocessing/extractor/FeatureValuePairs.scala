package org.silkframework.preprocessing.extractor

import scala.collection.mutable.{ArrayBuffer, HashMap}

/**
 * A FeatureValuePairs extractor.
 *
 */
class FeatureValuePairs(override val id:String,
                        override val propertyToExtractFrom:String,
                        override val transformers:List[Transformer],
                        override val propertyForTraining:String) extends AutoExtractor{


  val map = new HashMap[String,ArrayBuffer[String]]

  override def train(dataset:Dataset)={
    val filteredEntities = dataset.filter(propertyForTraining)


    for(entity <- filteredEntities){
      for(property <- entity.properties){
        if(map.isEmpty){
          map += property.path -> ArrayBuffer.empty[String]
          map(property.path) += property.value.toLowerCase
        }
        else{
          if(map.contains(property.path)){
            if(!map(property.path).contains(property.value))
              map(property.path) += property.value.toLowerCase
          }
          else{
            map += property.path -> ArrayBuffer.empty[String]
            map(property.path) += property.value.toLowerCase
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


  def apply(dataset:Dataset, findNewProperty: String=>String):Traversable[Entity]= {

    val filteredEntities = dataset.filter(propertyToExtractFrom)

    val newProperty = findNewProperty(propertyForTraining)

    for(entity <- filteredEntities) yield {
      val extractedProperties = for(property <- entity.properties) yield{
        val values = applyTransformation(List(property.value)).filter(value => checkModel(value)).toList.distinct
        new Property(newProperty, values.headOption.getOrElse(""))
      }
      new Entity(entity.uri, extractedProperties)
    }
  }

}
