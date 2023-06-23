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

import org.silkframework.config.Prefixes
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.entity.rdf.{SparqlEntitySchema, SparqlRestriction}

/**
 * A restriction for filtering datasets.
  *

  * @param operator The root operator.
 */
case class Restriction(operator: Option[Restriction.Operator]) {

  /** True if this restriction is empty, i.e., no filtering should be applied. */
  def isEmpty: Boolean = operator.isEmpty

  /** Retrieves all paths that are used by this restriction. */
  def paths: Set[UntypedPath] = operator.map(_.paths).getOrElse(Set.empty)

  def serialize: String = operator.map(_.serialize).mkString

  override def toString: String = operator.mkString
}

/**
 * Contains the available restriction operators and methods for parsing restrictions.
 */
object Restriction {

  /**
   * Returns an empty restriction
   */
  def empty: Restriction = Restriction(None)

  def custom(restriction: String)(implicit prefixes: Prefixes): Restriction = {
    if(restriction.trim.nonEmpty) {
      val sparqlRestriction = SparqlRestriction.fromSparql(SparqlEntitySchema.variable, restriction).toSparql
      Restriction(Some(CustomOperator(sparqlRestriction)))
    } else {
      Restriction.empty
    }
  }

  /**
   * Parses a condition.
   * Currently all conditions are parsed into custom conditions.
   */
  def parse(restriction: String)(implicit prefixes: Prefixes): Restriction = {
    if(restriction.trim.isEmpty)
      Restriction.empty
    else
      Restriction.custom(restriction)
  }

  /**
   * Base trait for all restriction operators.
   * Either a custom operator or a logical operator.
   */
  sealed trait Operator {

    def paths: Set[UntypedPath]

    def serialize: String
  }

  /**
   * A custom restriction operator.
   * The semantic interpretation of this operator depends on the implementing data set.
   * It can be used to express conditions that cannot be expressed with the provided logical operators.
   * Examples are: SQL or SPARQL patterns.
   */
  case class CustomOperator(expression: String) extends Operator {

    def paths: Set[UntypedPath] = Set.empty

    def serialize: String = expression
  }

  /**
   * A logical restriction operator.
   */
  sealed trait LogicalOperator extends Operator

  /**
   * A condition which evaluates to true if the provided path contains the given value.
   */
  case class Condition(path: UntypedPath, value: String) extends LogicalOperator {

    def paths: Set[UntypedPath] = Set(path)

    def serialize: String = s"$path = $value"
  }

  /**
   * Negates the provided operator.
   * Currently not supported.
   */
  case class Not(op: Operator) extends LogicalOperator {

    def paths: Set[UntypedPath] = op.paths

    def serialize: String = "!" + op.serialize
  }

  /**
   * Evaluates to true if all provided operators evaluate to true.
   */
  case class And(children: Iterable[Operator]) extends LogicalOperator {

    def paths: Set[UntypedPath] = children.flatMap(_.paths).toSet

    def serialize: String = children.mkString(" & ")
  }

  /**
   * Evaluates to true if at least one of the provided operators evaluate to true.
   */
  case class Or(children: Iterable[Operator]) extends LogicalOperator {

    def paths: Set[UntypedPath] = children.flatMap(_.paths).toSet

    def serialize: String = children.mkString(" ' ")

  }

}

