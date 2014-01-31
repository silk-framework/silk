package de.fuberlin.wiwiss.silk.preprocessing.config

import scala.xml.Node

/**
 * Created by Petar on 30/01/14.
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
