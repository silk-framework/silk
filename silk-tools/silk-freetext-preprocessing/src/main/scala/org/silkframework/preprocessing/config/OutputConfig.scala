package org.silkframework.preprocessing.config

import scala.xml.Node

/**
 * A Free text preprocessing configuration.
 * Specifies an output source.
 *
 * @param id The output id
 * @param file The file path
 * @param format The format of the file
 */
case class OutputConfig(id: String,
                        file: String,
                        format: String) {

}


object OutputConfig{


  def fromXML(node:Node):OutputConfig = {
    val id = node \ "@id" text
    val params = readParams(node)
    val file = params.get("file").getOrElse( throw new Exception )
    val format = params.get("format").getOrElse( throw new Exception )
    OutputConfig(id, file, format)
  }

  private def readParams(element: Node): Map[String, String] = {
    element \ "Param" map (p => (p \ "@name" text, p \ "@value" text)) toMap
  }
}