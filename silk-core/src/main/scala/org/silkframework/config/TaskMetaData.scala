package org.silkframework.config

import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat, XmlSerialization}

import scala.xml._

/**
  * Holds meta data about a task.
  */
case class TaskMetaData(label: String, description: String) {

}

object TaskMetaData {

  def empty = TaskMetaData("", "")

  /**
    * XML serialization format.
    */
  implicit object TaskMetaDataFormat extends XmlFormat[TaskMetaData] {
    /**
      * Deserialize a value from XML.
      */
    def read(node: Node)(implicit readContext: ReadContext) = {
      TaskMetaData(
        label = (node \ "Label").text,
        description = (node \ "Description").text
      )
    }

    /**
      * Serialize a value to XML.
      */
    def write(data: TaskMetaData)(implicit writeContext: WriteContext[Node]): Node = {
      <MetaData>
        <Label>{data.label}</Label>
        <Description>{data.description}</Description>
      </MetaData>
    }
  }

}