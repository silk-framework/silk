package org.silkframework.preprocessing.extractor

import org.silkframework.preprocessing.transformer.Transformer
import org.silkframework.preprocessing.entity.{Property, Entity}
import scala.io.Source
import org.silkframework.preprocessing.dataset.Dataset

/**
 * A Dictionary extractor.
 *
 */
case class Dictionary(override  val id: String,
                      override val propertyToExtractFrom: String,
                      override val transformers:List[Transformer],
                      override val param:String) extends ManualExtractor{


  val values = Source.fromFile(param).getLines.mkString("\n")


  def solvePath(s: String) = {
    "(?<=[A-Z])(?=[A-Z][a-z])".r.findAllIn(s).subgroups(0)
  }

  override def apply(dataset:Dataset, findNewProperty: String => String):Traversable[Entity] = {


    //TODO: FIX!!!
    val filteredEntities = dataset.filter(propertyToExtractFrom)

    val newProperty = findNewProperty(solvePath(id))

    for(entity <- filteredEntities) yield {
      val extractedProperties = for(property <- entity.properties) yield{

        new Property(newProperty, values)
      }
      new Entity(entity.uri, extractedProperties)
    }
  }

}
