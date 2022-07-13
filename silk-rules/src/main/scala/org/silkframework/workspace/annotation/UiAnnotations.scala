package org.silkframework.workspace.annotation

import org.silkframework.runtime.plugin.PluginObjectParameterNoSchema
import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat, XmlSerialization}

import scala.xml.Node

/** Annotations that are displayed in the UI. */
case class UiAnnotations(stickyNotes: Seq[StickyNote] = Seq.empty) extends PluginObjectParameterNoSchema

object UiAnnotations {
  implicit object UiAnnotationsXmlFormat extends XmlFormat[UiAnnotations] {
    override def read(xml: Node)(implicit readContext: ReadContext): UiAnnotations = {
      val stickyNotes = (xml \ "StickyNotes" \ "StickyNote").map(XmlSerialization.fromXml[StickyNote])
      UiAnnotations(stickyNotes)
    }

    override def write(annotations: UiAnnotations)(implicit writeContext: WriteContext[Node]): Node = {
      <UiAnnotations>
        <StickyNotes>
          {annotations.stickyNotes.map(XmlSerialization.toXml[StickyNote])}
        </StickyNotes>
      </UiAnnotations>
    }
  }
}