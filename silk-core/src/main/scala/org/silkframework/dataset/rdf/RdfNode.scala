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

package org.silkframework.dataset.rdf

/**
 * An RDF node, which is one of: Resource, BlankNode, Literal.
 */
sealed trait RdfNode {
  /**
   * The value of this node.
   */
  val value: String
}

/**
  * An RdfNode which is either a Resource or a BlankNode
  */
sealed trait ConcreteNode extends RdfNode

/**
 * An RDF resource.
 */
case class Resource(value: String) extends ConcreteNode

/**
 * An RDF blank node.
 */
case class BlankNode(value: String) extends ConcreteNode

/**
 * An RDF literal.
 */
sealed trait Literal extends RdfNode

/** A plain literal */
case class PlainLiteral(value: String) extends Literal

/** A language literal */
case class LanguageLiteral(value: String, language: String) extends Literal

/** A data type literal */
case class DataTypeLiteral(value: String, dataType: String) extends Literal
