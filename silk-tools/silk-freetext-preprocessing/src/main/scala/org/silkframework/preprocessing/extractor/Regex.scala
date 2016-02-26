package org.silkframework.preprocessing.extractor

/**
 * A Regex extractor.
 *
 */
case class Regex(override  val id: String,
                 override val propertyToExtractFrom: String,
                 override val transformers:List[Transformer],
                 override val param: String) extends ManualExtractor{
  private[this] val compiledRegex = param.r



  def solvePath(s: String) = {
    val regex = "(.*)Extractor".r.findAllIn(s)
    if(regex.hasNext) regex.subgroups(0) else ""
  }

  override def apply(dataset:Dataset, findNewProperty: String => String):Traversable[Entity]= {

    val filteredEntities = dataset.filter(propertyToExtractFrom)

    val newProperty = findNewProperty(solvePath(id))

    for(entity <- filteredEntities) yield {
      val extractedProperties = for(property <- entity.properties) yield{
        val values = compiledRegex.findAllIn(applyTransformation(List(property.value)).mkString(" "))
        val value = if(values.hasNext) values.next() else ""
        new Property(newProperty, value)
      }
      new Entity(entity.uri, extractedProperties)
    }
  }
}
