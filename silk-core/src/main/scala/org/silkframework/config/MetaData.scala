package org.silkframework.config

import java.time.Instant
import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat}
import scala.xml._

/**
  * Holds meta data about a task.
  */
case class MetaData(label: String, description: String, modified: Instant) {

}

object MetaData {

  def empty = MetaData("", "", Instant.now)

  /**
    * XML serialization format.
    */
  implicit object MetaDataFormat extends XmlFormat[MetaData] {
    /**
      * Deserialize a value from XML.
      */
    def read(node: Node)(implicit readContext: ReadContext): MetaData = {
      MetaData(
        label = (node \ "Label").text,
        description = (node \ "Description").text,
        modified = (node \ "Modified").headOption.map(node => Instant.parse(node.text)).getOrElse(Instant.now)
      )
    }

    /**
      * Serialize a value to XML.
      */
    def write(data: MetaData)(implicit writeContext: WriteContext[Node]): Node = {
      <MetaData>
        <Label>{data.label}</Label>
        <Description>{data.description}</Description>
        <Modified>{data.modified.toString}</Modified>
      </MetaData>
    }
  }

}