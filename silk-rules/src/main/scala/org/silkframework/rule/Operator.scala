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

import org.silkframework.runtime.plugin.ParameterValues
import org.silkframework.runtime.serialization.XmlSerialization
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.Identifier

import java.util.concurrent.atomic.AtomicInteger
import scala.xml.Node

/**
 * Base class of all rule operators.
 */
trait Operator {
  /**
   * The identifier of this operator.
   */
  def id: Identifier

  /**
    * The children operators.
    */
  def children: Seq[Operator]

  /**
    * Generates the same operator with a new identifier.
    */
  def withId(newId: Identifier): Operator

  /**
    * Generates the same operator with new children.
    */
  def withChildren(newChildren: Seq[Operator]): Operator

  /**
   * Builds an executor that performs this operator's runtime work for a given task context.
   * The returned [[OperatorExecution]] holds any state that depends on the task context
   * (e.g. resolved input task data). Pure operators may return themselves.
   */
  def execution(taskContext: TaskContext): OperatorExecution

  /**
    * Asserts that all identifiers in this rule tree are unique.
    *
    * @throws ValidationException If duplicate identifiers have been found.
    */
  final def validateIds(): Unit = {
    val allIds = RuleTraverser(this).iterateAllChildren.map(_.operator.id).toList
    val duplicateIds = allIds.groupBy(_.toString).filter(_._2.size > 1).keys
    if (duplicateIds.nonEmpty) {
      throw new ValidationException("Duplicate identifiers found in rule: " + duplicateIds.mkString(", "))
    }
  }

}

/**
 * Runtime form of an [[Operator]], contextualized for a specific [[TaskContext]].
 * Each [[Operator]] subclass has a corresponding [[OperatorExecution]] sub-trait
 * (e.g. [[org.silkframework.rule.input.InputExecution]],
 * [[org.silkframework.rule.similarity.SimilarityOperatorExecution]])
 * that declares the actual execution methods.
 */
trait OperatorExecution {
  /** Backlink to the operator that produced this execution. */
  def operator: Operator
}

/**
 * Operator companion object.
 */
object Operator {

  /** Counter used to generate unique identifiers. */
  private val lastId = new AtomicInteger(0)

  /**
   * Generates a new operator identifier.
   */
  def generateId: Identifier = Identifier("unnamed_" + lastId.incrementAndGet())

  /**
   * Reads the operator identifier from an xml element.
   */
  def readId(xml: Node): Identifier = {
    (xml \ "@id").headOption.map(_.text).map(Identifier(_)).getOrElse(generateId)
  }

  /**
   * Reads the parameters of an operator.
   */
  def readParams(node: Node): ParameterValues = {
    XmlSerialization.deserializeParameters(node)
  }
}