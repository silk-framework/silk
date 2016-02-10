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
import org.silkframework.util.Uri

/**
 * Represents an operator in an RDF path.
 */
sealed abstract class PathOperator {
  /**
   * Serializes this operator using the Silk RDF path language.
   */
  def serialize(implicit prefixes: Prefixes = Prefixes.empty): String

  override def toString = serialize(Prefixes.empty)
}

/**
 * Moves forward from a subject resource (set) through a property to its object resource (set).
 */
case class ForwardOperator(property: Uri) extends PathOperator {
  override def serialize(implicit prefixes: Prefixes) = "/" + property.serialize
}

/**
 * Moves backward from an object resource (set) through a property to its subject resource (set).
 */
case class BackwardOperator(property: Uri) extends PathOperator {
  override def serialize(implicit prefixes: Prefixes) = "\\" + property.serialize
}

/**
 * Reduces the currently selected set of resources to the ones with a specific language.
 *
 * @param operator Comparison operator. May be one of =, !=.
 * @param language The language.
 */
case class LanguageFilter(operator: String, language: String) extends PathOperator {
  override def serialize(implicit prefixes: Prefixes) = "[@lang " + operator + " " + language + "]"
}

/**
 * Reduces the currently selected set of resources to the ones matching the filter expression.
 *
 * @param property The property which has the values which should be use for filtering
 * @param operator Comparison operator. May be one of >, <, >=, <=, =, !=.
 * @param value The comparison value.
 *              URIs must be enclosed in angle brackets, e.g., <URI>.
 *              String values must be enclosed in quotes, e.g., "Value"
 */
case class PropertyFilter(property: Uri, operator: String, value: String) extends PathOperator {
  override def serialize(implicit prefixes: Prefixes) = "[" + property.serialize + " " + operator + " " + value + "]"

  def evaluate(v: String): Boolean = {
    operator match {
      case "=" => v == value
      case "!=" => v != value
      case ">" => v > value
      case "<" => v < value
      case ">=" => v >= value
      case "<=" => v <= value
    }
  }
}
