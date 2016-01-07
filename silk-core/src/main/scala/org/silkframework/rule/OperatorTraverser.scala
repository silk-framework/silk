/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.silkframework.rule

/**
 * Allows to traverse through a rule tree while editing specific operators.
 */
sealed trait OperatorTraverser {

  /** The current operator. */
  val operator: Operator

  /** Updates the current operator. */
  def update(updatedOperator: Operator): OperatorTraverser

  /** Moves one operator to the left. */
  def moveLeft: Option[OperatorTraverser]

  /** Moves one operator to the right. */
  def moveRight: Option[OperatorTraverser]

  /** Moves one operator up. */
  def moveUp: Option[OperatorTraverser]

  /** Moves one operator down. */
  def moveDown: Option[OperatorTraverser] = {
    if (operator.children.isEmpty) {
      None
    }
    else {
      Some(OperatorTraverser.Hole(operator.children.head, Nil, this, operator.children.tail))
    }
  }

  /** Returns the root operator */
  def root: OperatorTraverser = iterate(_.moveUp).toTraversable.last

  /** Iterates through all direct children */
  def iterateChildren = moveDown match {
    case Some(down) => down.iterate(_.moveRight)
    case None => Iterator.empty
  }

  /** Iterates through all descendant operators (i.e. the operator itself and all of its direct and indirect children) */
  def iterateAll: Iterator[OperatorTraverser] = {
    Iterator.single(this) ++ iterateChildren.flatMap(_.iterateAll)
  }

  def iterate(f: OperatorTraverser => Option[OperatorTraverser]) = new Iterator[OperatorTraverser] {

    private var currentOperator: Option[OperatorTraverser] = Some(OperatorTraverser.this)

    def hasNext = currentOperator.isDefined

    def next() = currentOperator match {
      case Some(o) =>
        currentOperator = f(o)
        o
      case None => throw new NoSuchElementException("next on empty iterator")
    }
  }
}

/**
  * Allows to traverse through a rule tree while editing specific operators.
  */
object OperatorTraverser {

  /**
    * Creates a new rule traverser.
    *
    * @param operator The root operator from which the traversal starts.
    */
  def apply(operator: Operator) = Root(operator)

  /**
    * Given a rule traverser, returns the current operator.
    */
  def unapply(location: OperatorTraverser): Option[Operator] = Some(location.operator)

  case class Root(override val operator: Operator) extends OperatorTraverser {

    override def update(updatedOperator: Operator) = Root(updatedOperator)

    override def moveLeft = None

    override def moveRight = None

    override def moveUp = None
  }

  case class Hole(override val operator: Operator, left: Seq[Operator], parent: OperatorTraverser, right: Seq[Operator]) extends OperatorTraverser {

    override def update(updatedOperator: Operator) = {
      val updatedParent = parent.update(parent.operator.withChildren(left ++ (updatedOperator +: right)))

      Hole(updatedOperator, left, updatedParent, right)
    }

    override def moveLeft = left match {
      case leftHead :: leftTail => Some(Hole(leftHead, leftTail, parent, operator +: right))
      case Nil => None
    }

    override def moveRight = right match {
      case rightHead :: rightTail => Some(Hole(rightHead, operator +: left, parent, rightTail))
      case Nil => None
    }

    override def moveUp = Some(parent)
  }

}