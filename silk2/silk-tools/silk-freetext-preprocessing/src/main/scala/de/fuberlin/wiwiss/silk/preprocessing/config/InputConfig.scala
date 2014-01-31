package de.fuberlin.wiwiss.silk.preprocessing.config

import scala.xml.Node

/**
 * Created by Petar on 30/01/14.
 */
case class InputConfig(id: String,
                       file: String,
                       format: String,
                       inType: String) {

}

object InputConfig{


  def fromXML(node:Node):InputConfig = {
    val id = node \ "@id" text
    val params = readParams(node)
    val file = params.get("file").getOrElse( throw new Exception )
    val format = params.get("format").getOrElse( throw new Exception )
    new InputConfig(id, file, format, node.text)
  }

  private def readParams(element: Node): Map[String, String] = {
    element \ "Param" map (p => (p \ "@name" text, p \ "@value" text)) toMap
  }
}
