package de.fuberlin.wiwiss.silk.workbench.learning.tree

/**
 * Used to traverse through a node tree and edit specific nodes.
 */
sealed trait NodeTraverser {
  /**The current node. */
  val node: Node

  /**Updates the current node. */
  def update(updatedNode: Node): NodeTraverser

  /**Moves one node to the left. */
  def moveLeft: Option[NodeTraverser]

  /**Moves one node to the right. */
  def moveRight: Option[NodeTraverser]

  /**Moves one node up. */
  def moveUp: Option[NodeTraverser]

  /**Moves one node down. */
  def moveDown: Option[NodeTraverser] = {
    if (node.children.isEmpty) {
      None
    }
    else {
      Some(NodeTraverser.Hole(node.children.head, Nil, this, node.children.tail))
    }
  }

  /**Returns the root node */
  def root: NodeTraverser = iterate(_.moveUp).toTraversable.last

  /**Iterates through all direct children */
  def iterateChildren = moveDown match {
    case Some(down) => down.iterate(_.moveRight)
    case None => Iterator.empty
  }

  /**Iterates through all descendant nodes (i.e. the node itself and all of its direct and indirect children) */
  def iterateAll: Iterator[NodeTraverser] = {
    Iterator.single(this) ++ iterateChildren.flatMap(_.iterateAll)
  }

  def iterate(f: NodeTraverser => Option[NodeTraverser]) = new Iterator[NodeTraverser] {
    private var currentNode: Option[NodeTraverser] = Some(NodeTraverser.this)

    def hasNext = currentNode.isDefined

    def next() = currentNode match {
      case Some(node) => {
        currentNode = f(node)
        node
      }
      case None => throw new NoSuchElementException("next on empty iterator")
    }
  }
}

object NodeTraverser {
  def apply(node: Node) = Root(node)

  def unapply(location: NodeTraverser): Option[Node] = Some(location.node)

  case class Root(override val node: Node) extends NodeTraverser {
    override def update(updatedNode: Node) = Root(updatedNode)

    override def moveLeft = None

    override def moveRight = None

    override def moveUp = None
  }

  case class Hole(override val node: Node, left: List[Node], parent: NodeTraverser, right: List[Node]) extends NodeTraverser {
    override def update(updatedNode: Node) = {
      val updatedParent = parent.update(parent.node.updateChildren(left ::: updatedNode :: right))

      Hole(updatedNode, left, updatedParent, right)
    }

    override def moveLeft = left match {
      case leftHead :: leftTail => Some(Hole(leftHead, leftTail, parent, node :: right))
      case Nil => None
    }

    override def moveRight = right match {
      case rightHead :: rightTail => Some(Hole(rightHead, node :: left, parent, rightTail))
      case Nil => None
    }

    override def moveUp = Some(parent)
  }

}