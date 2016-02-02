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

package org.silkframework.entity

/**
 * A restriction for filtering datasets.

 * @param operator The root operator.
 */
case class Restriction(operator: Option[Restriction.Operator]) {

  /** True if this restriction is empty, i.e., no filtering should be applied. */
  def isEmpty = operator.isEmpty

  /** Retrieves all paths that are used by this restriction. */
  def paths = operator.map(_.paths).getOrElse(Set.empty)

  override def toString = operator.mkString
}

/**
 * Contains the available restriction operators and methods for parsing restrictions.
 */
object Restriction {

  /**
   * Returns an empty restriction
   */
  def empty = Restriction(None)

  /**
   * Parses a condition.
   * Currently all conditions are parsed into custom conditions.
   */
  def parse(restriction: String) = Restriction(Some(CustomOperator(restriction)))

  /**
   * Base trait for all restriction operators.
   * Either a custom operator or a logical operator.
   */
  sealed trait Operator {

    def paths: Set[Path]

    def serialize: String
  }

  /**
   * A custom restriction operator.
   * The semantic interpretation of this operator depends on the implementing data set.
   * It can be used to express conditions that cannot be expressed with the provided logical operators.
   * Examples are: SQL or SPARQL patterns.
   */
  case class CustomOperator(expression: String) extends Operator {

    def paths: Set[Path] = Set.empty

    def serialize = expression
  }

  /**
   * A logical restriction operator.
   */
  trait LogicalOperator extends Operator

  /**
   * A condition which evaluates to true if the provided path contains the given value.
   */
  case class Condition(path: Path, value: String) extends LogicalOperator {

    def paths = Set(path)

    def serialize = s"$path = $value"
  }

  /**
   * Negates the provided operator.
   * Currently not supported.
   */
  case class Not(op: Operator) extends LogicalOperator {

    def paths = op.paths

    def serialize = "!" + op.serialize
  }

  /**
   * Evaluates to true if all provided operators evaluate to true.
   */
  case class And(children: Traversable[Operator]) extends LogicalOperator {

    def paths = children.flatMap(_.paths).toSet

    def serialize = children.mkString(" & ")
  }

  /**
   * Evaluates to true if at least one of the provided operators evaluate to true.
   */
  case class Or(children: Traversable[Operator]) extends LogicalOperator {

    def paths = children.flatMap(_.paths).toSet

    def serialize = children.mkString(" ' ")

  }

}

