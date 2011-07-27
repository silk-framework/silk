package de.fuberlin.wiwiss.silk.learning.individual

import util.Random
import javax.naming.OperationNotSupportedException

trait Node {
  val children: List[Node] = Nil

  def updateChildren(children: List[Node]): Node = {
    if (children.isEmpty) {
      this
    }
    else {
      throw new OperationNotSupportedException("Cannot have any child nodes")
    }
  }
}
