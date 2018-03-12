package org.silkframework.config

import java.time.Instant
import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat}
import scala.xml._

/**
  * Holds meta data about a task.
  */
case class MetaData(label: String, description: String, modified: Option[Instant] = None) {

}

object MetaData {

  def empty = MetaData("", "")

  /**
    * XML serialization format.
    */
  implicit object MetaDataXmlFormat extends XmlFormat[MetaData] {
    /**
      * Deserialize a value from XML.
      */
    def read(node: Node)(implicit readContext: ReadContext): MetaData = {
      MetaData(
        label = (node \ "Label").text,
        description = (node \ "Description").text,
        modified = (node \ "Modified").headOption.map(node => Instant.parse(node.text))
      )
    }

    /**
      * Serialize a value to XML.
      */
    def write(data: MetaData)(implicit writeContext: WriteContext[Node]): Node = {
      <MetaData>
        <Label>{data.label}</Label>
        <Description>{data.description}</Description>
        { data.modified.map(instant => <Modified>{instant.toString}</Modified>).toSeq }
      </MetaData>
    }
  }

}