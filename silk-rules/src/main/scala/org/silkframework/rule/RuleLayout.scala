package org.silkframework.rule

import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat}

import scala.xml.Node

/** Rule layout data, i.e. how a linkage rule should be shown in a UI.
  *
  * @param nodePositions The positions (x, y) of each rule node in the rule editor.
  */
case class RuleLayout(nodePositions: Map[String, (Int, Int)] = Map.empty)

object RuleLayout {
  private def textToInt(text: String): Int = Math.round(text.toDouble).toInt
  implicit object RuleLayoutXmlFormat extends XmlFormat[RuleLayout] {
    override def read(layoutNode: Node)(implicit readContext: ReadContext): RuleLayout = {
      val nodePositionsXml = layoutNode \ "NodePositions" \ "NodePos"
      val positions = nodePositionsXml.map(nodePos => {
        val nodeId = (nodePos \ "@id").text
        val x = textToInt((nodePos \ "@x").text)
        val y = textToInt((nodePos \ "@y").text)
        (nodeId, (x, y))
      })
      RuleLayout(positions.toMap)
    }

    override def write(value: RuleLayout)(implicit writeContext: WriteContext[Node]): Node = {
      <RuleLayout>
        <NodePositions>
          {value.nodePositions.map { case (nodeId, (x, y)) =>
            <NodePos id={nodeId} x={x.toString} y={y.toString} />
          }}
        </NodePositions>
      </RuleLayout>
    }
  }
}