package org.silkframework.rule

import org.silkframework.util.Identifier
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class RuleTraverserTest extends AnyFlatSpec with Matchers {

  behavior of "RuleTraverser"

  it should "support moving in the tree" in {
    val tree = Node("Root", Seq(Node("A"), Node("B"), Node("C")))
    val root = RuleTraverser(tree)

    val nodeA = root.moveDown.get
    nodeA.operator.id shouldBe "A"

    val nodeB = nodeA.moveRight.get
    nodeB.operator.id shouldBe "B"

    val nodeC = nodeB.moveRight.get
    nodeC.operator.id shouldBe "C"

    val nodeB2 = nodeC.moveLeft.get
    nodeB2.operator.id shouldBe "B"

    val nodeA2 = nodeB2.moveLeft.get
    nodeA2.operator.id shouldBe "A"

    val root2 = nodeB2.moveUp.get
    root2.operator.id shouldBe "Root"
  }

  case class Node(id: Identifier, children: Seq[Node] = Seq.empty) extends Operator {
    override def withChildren(newChildren: Seq[Operator]): Operator = copy(children = newChildren.map(_.asInstanceOf[Node]))
  }

}
