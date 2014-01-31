package de.fuberlin.wiwiss.silk.preprocessing.config

import scala.xml.Node

/**
 * Created by Petar on 30/01/14.
 */
case class OutputConfig(id: String,
                        file: String,
                        format: String) {

    def outputId = id

    def outputFile = file

    def outputFormat = format
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