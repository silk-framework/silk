package org.silkframework.config

import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat, XmlSerialization}

import scala.xml._

/**
  * Holds meta data about a task.
  */
case class MetaData(label: String, description: String) {

}

object MetaData {

  def empty = MetaData("", "")

  /**
    * XML serialization format.
    */
  implicit object MetaDataFormat extends XmlFormat[MetaData] {
    /**
      * Deserialize a value from XML.
      */
    def read(node: Node)(implicit readContext: ReadContext) = {
      MetaData(
        label = (node \ "Label").text,
        description = (node \ "Description").text
      )
    }

    /**
      * Serialize a value to XML.
      */
    def write(data: MetaData)(implicit writeContext: WriteContext[Node]): Node = {
      <MetaData>
        <Label>{data.label}</Label>
        <Description>{data.description}</Description>
      </MetaData>
    }
  }

}