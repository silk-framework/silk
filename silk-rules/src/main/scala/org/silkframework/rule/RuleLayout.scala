package org.silkframework.rule

import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat}

import scala.xml.Node

/** Rule layout data, i.e. how a linkage rule should be shown in a UI.
  *
  * @param nodePositions The position (x, y) and dimensions of each rule node in the rule editor.
  */
case class RuleLayout(nodePositions: Map[String, NodePosition] = Map.empty)

object RuleLayout {
  private def textToInt(text: String): Int = Math.round(text.toDouble).toInt
  implicit object RuleLayoutXmlFormat extends XmlFormat[RuleLayout] {
    override def read(layoutNode: Node)(implicit readContext: ReadContext): RuleLayout = {
      val nodePositionsXml = layoutNode \ "NodePositions" \ "NodePos"
      val positions = nodePositionsXml.map(nodePos => {
        val nodeId = (nodePos \ "@id").text
        val x = textToInt((nodePos \ "@x").text)
        val y = textToInt((nodePos \ "@y").text)
        val width = (nodePos \ "@width").headOption.map(n => textToInt(n.text))
        val height = (nodePos \ "@height").headOption.map(n => textToInt(n.text))
        (nodeId, NodePosition(x, y, width, height))
      })
      RuleLayout(positions.toMap)
    }

    override def write(value: RuleLayout)(implicit writeContext: WriteContext[Node]): Node = {
      <RuleLayout>
        <NodePositions>
          { value.nodePositions.map { case (nodeId, pos) =>
              <NodePos id={nodeId} x={pos.x.toString} y={pos.y.toString} width={pos.width.map(_.toString).orNull} height={pos.height.map(_.toString).orNull} />
          }}
        </NodePositions>
      </RuleLayout>
    }
  }
}

/**
 * Holds the position and the width of an operator.
 *
 * @param x The x coordinate
 * @param y The y coordinate
 * @param width An optional used-defined width. If not provided, the width should be determined by the UI.
 * @param height An optional used-defined height. If not provided, the width should be determined by the UI.
 */
case class NodePosition(x: Int, y: Int, width: Option[Int] = None, height: Option[Int] = None)