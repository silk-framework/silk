package org.silkframework.config

import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat}

import scala.xml.Node

case class Tag(uri: String, label: String)

case class TagReference(uri: String)

object Tag {

  /**
    * XML serialization format.
    */
  implicit object TagXmlFormat extends XmlFormat[Tag] {
    /**
      * Deserialize a value from XML.
      */
    def read(node: Node)(implicit readContext: ReadContext): Tag = {
      Tag(
        uri = (node \ "@uri").text,
        label = (node \ "Label").text
      )
    }

    /**
      * Serialize a value to XML.
      */
    def write(tag: Tag)(implicit writeContext: WriteContext[Node]): Node = {
      <Tag uri={tag.uri}>
        <Label>{tag.label}</Label>
      </Tag>
    }
  }

}