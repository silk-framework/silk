package org.silkframework.rule

import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat}

import scala.xml.Node

/** Rule layout data, i.e. how a linkage rule should be shown in a UI.
  *
  * @param nodePositions The position (x, y) and width of each rule node in the rule editor.
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
        (nodeId, NodePosition(x, y, width))
      })
      RuleLayout(positions.toMap)
    }

    override def write(value: RuleLayout)(implicit writeContext: WriteContext[Node]): Node = {
      <RuleLayout>
        <NodePositions>
          { value.nodePositions.map {
              case (nodeId, NodePosition(x, y, Some(width))) =>
                  <NodePos id={nodeId} x={x.toString} y={y.toString} width={width.toString}/>
              case (nodeId, NodePosition(x, y, None)) =>
                  <NodePos id={nodeId} x={x.toString} y={y.toString}/>
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
 */
case class NodePosition(x: Int, y: Int, width: Option[Int] = None)