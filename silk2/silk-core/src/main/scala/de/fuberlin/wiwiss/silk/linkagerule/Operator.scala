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

package de.fuberlin.wiwiss.silk.linkagerule

import scala.xml.Node
import de.fuberlin.wiwiss.silk.util.Identifier
import java.util.concurrent.atomic.AtomicInteger

/**
 * Base class of all operators in the link condition.
 */
trait Operator {
  /**
   * The identifier of this operator.
   */
  val id: Identifier
}

/**
 * Operator companion object.
 */
object Operator {
  /**Counter used to generate unique identifiers. */
  private val lastId = new AtomicInteger(0)

  /**
   * Generates a new operator identifier.
   */
  def generateId = Identifier("unnamed_" + lastId.incrementAndGet())

  /**
   * Reads the operator identifier from an xml element.
   */
  def readId(xml: Node): Identifier = {
    (xml \ "@id").headOption.map(_.text).map(Identifier(_)).getOrElse(generateId)
  }

  /**
   * Reads the parameters of an operator.
   */
  def readParams(node: Node): Map[String, String] = {
    (node \ "Param").map(p => ((p \ "@name").text, (p \ "@value").text)).toMap
  }
}