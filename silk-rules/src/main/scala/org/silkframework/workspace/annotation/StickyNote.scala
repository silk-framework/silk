package org.silkframework.workspace.annotation

import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat}

import scala.xml.{Node, PCData}

/** Visual sticky note that can be displayed in different places, like the rule editors.
  *
  * @param id        The ID of the sticky note.
  * @param content   The text content of the note.
  * @param color     Color of the note.
  * @param position  Position of the upper-left corner.
  * @param dimension Width and height.
  */
case class StickyNote(id: String,
                      content: String,
                      color: String,
                      position: (Double, Double),
                      dimension: (Double, Double))

object StickyNote {
  implicit object StickyNodeXmlFormat extends XmlFormat[StickyNote] {
    override def read(xml: Node)(implicit readContext: ReadContext): StickyNote = {
      val id = (xml \ "@id").text
      val color = (xml \ "@color").text
      val content = (xml \ "Content").text
      val position = ((xml \ "Position" \ "@x").text.toDouble, (xml \ "Position" \ "@y").text.toDouble)
      val dimension = ((xml \ "Dimension" \ "@width").text.toDouble, (xml \ "Dimension" \ "@height").text.toDouble)
      StickyNote(id, content, color, position, dimension)
    }

    override def write(stickyNote: StickyNote)(implicit writeContext: WriteContext[Node]): Node = {
      val content = PCData(stickyNote.content)
      <StickyNote id={stickyNote.id} color={stickyNote.color}>
        <Content xml:space="preserve">{content}</Content>
        <Position x={stickyNote.position._1.toString} y={stickyNote.position._2.toString} />
        <Dimension width={stickyNote.dimension._1.toString} height={stickyNote.dimension._2.toString} />
      </StickyNote>
    }
  }
}