package org.silkframework.preprocessing.config

import scala.xml.Node

/**
 * Represents a Extractor Configuration from XML.
 * Specifies a single extractor.
 *
 * @param id The extractor id
 * @param method The extraction method
 * @param property The property path to extract from
 * @param transformers Transformations specification to be done on the free text
 * @param param A additional parameter depending on the extractor
 */
case class ExtractorConfig(id: String,
                           method: String,
                           property: String,
                           transformers: Option[TransformerConfig],
                           param: String) {


}

object ExtractorConfig{

  /**
   * Reads a Extractor Configuration from XML.
   */
  def fromXML(node:Node):ExtractorConfig = {
    //Read id
    val id = (node \ "@id").text

    //Read method
    val method = (node \ "@method").text

    //Read property to extract from
    val propertyForExtracting = (node \ "PropertyToExtractFrom" \ "@name").text
    val isTransformerDefined = (node \ "PropertyToExtractFrom" \ "Transform").headOption.isDefined

    val transformers:Option[TransformerConfig] = isTransformerDefined match {
      case true => (node \ "PropertyToExtractFrom" \ "Transform").map(TransformerConfig.fromXML).headOption
      case false =>  None
    }

    val param = method match {
      case name if name == "BagOfWords" || name == "FeatureValuePairs" => getTrainingProperty(node)
      case name if name ==  "RegexMatch" || name == "DictionarySearch" => readParam(node)
    }
    new ExtractorConfig(id, method, propertyForExtracting, transformers, param)
  }

  private def readParam(element: Node): String = {
    (element \ "Param").map (p => p \ "@value" text).head
  }

  private def getTrainingProperty(node: Node):String = {
    val isPropTrainingDefined = (node \ "PropertyForTraining").headOption.isDefined

    isPropTrainingDefined match {
      case prop@ true => (node \ "PropertyForTraining" \ "@name").text
      case prop@ false => ""
    }
  }


}
