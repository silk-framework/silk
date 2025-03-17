package org.silkframework.workspace.annotation

import org.silkframework.rule.NodePosition
import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat}

import scala.xml.{Node, PCData}

/** Visual sticky note that can be displayed in different places, like the rule editors.
  *
  * @param id        The ID of the sticky note.
  * @param content   The text content of the note.
  * @param color     Color of the note.
  * @param nodePosition The position and dimension of the sticky note.
  */
case class StickyNote(id: String,
                      content: String,
                      color: String,
                      nodePosition: NodePosition)

object StickyNote {
  implicit object StickyNodeXmlFormat extends XmlFormat[StickyNote] {
    override def read(xml: Node)(implicit readContext: ReadContext): StickyNote = {
      val id = (xml \ "@id").text
      val color = (xml \ "@color").text
      val content = (xml \ "Content").text
      val x = (xml \ "Position" \ "@x").text.toInt
      val y = (xml \ "Position" \ "@y").text.toInt
      val widthOption = (xml \ "Dimension" \ "@width").headOption.filter(_.text.trim.nonEmpty).map(_.text.toDouble.toInt)
      val heightOption = (xml \ "Dimension" \ "@height").headOption.filter(_.text.trim.nonEmpty).map(_.text.toDouble.toInt)
      StickyNote(id, content, color, NodePosition(x, y, widthOption, heightOption))
    }

    override def write(stickyNote: StickyNote)(implicit writeContext: WriteContext[Node]): Node = {
      val content = PCData(stickyNote.content)
      <StickyNote id={stickyNote.id} color={stickyNote.color}>
        <Content xml:space="preserve">{content}</Content>
        <Position x={stickyNote.nodePosition.x.toString} y={stickyNote.nodePosition.y.toString} />
        <Dimension width={stickyNote.nodePosition.width.map(_.toString).getOrElse("")} height={stickyNote.nodePosition.height.map(_.toString).getOrElse("")} />
      </StickyNote>
    }
  }
}