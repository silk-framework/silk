package org.silkframework.preprocessing.config

import scala.xml.Node

/**
 * A Free text preprocessing configuration.
 * Specifies an output source.
 *
 * @param method The transform method
 * @param params A map of parameters needed for the transformer
 * @param transform An optional nested transform specification
 */
case class TransformerConfig(method: String,
                             params: Map[String,String],
                             transform: Option[TransformerConfig]) {

}

object TransformerConfig{

  def fromXML(node:Node):TransformerConfig = {

    //Read method
    val method = (node \ "@method").text

    //Read parameters
    val params = readParams(node)


    val subTransformer = (node \ "Transform").headOption
    if(subTransformer.isDefined){
      new TransformerConfig(method, params, (node \ "Transform").map(TransformerConfig.fromXML).headOption)
    }
    else{
      new TransformerConfig(method, params, None)
    }

  }

  private def readParams(element: Node): Map[String, String] = {
    element \ "Param" map (p => (p \ "@name" text, p \ "@value" text)) toMap
  }

}
