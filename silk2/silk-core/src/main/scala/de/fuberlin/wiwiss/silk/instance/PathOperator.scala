package de.fuberlin.wiwiss.silk.instance

import de.fuberlin.wiwiss.silk.util.Uri
import de.fuberlin.wiwiss.silk.config.Prefixes

/**
 * Represents an operator in an RDF path.
 */
sealed abstract class PathOperator {
  /**
   * Serializes this operator using the Silk RDF path language.
   */
  def serialize(implicit prefixes: Prefixes): String

  override def toString = serialize(Prefixes.empty)
}

/**
 * Moves forward from a subject resource (set) through a property to its object resource (set).
 */
case class ForwardOperator(property: Uri) extends PathOperator {
  override def serialize(implicit prefixes: Prefixes) = "/" + property.toTurtle
}

/**
 * Moves backward from an object resource (set) through a property to its subject resource (set).
 */
case class BackwardOperator(property: Uri) extends PathOperator {
  override def serialize(implicit prefixes: Prefixes) = "\\" + property.toTurtle
}

/**
 * Reduces the currently selected set of resources to the ones with a specific language.
 *
 * @param operator Comparison operator. May be one of >, <, >=, <=, =, !=.
 * @param value The language.
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
 */
case class PropertyFilter(property: String, operator: String, value: String) extends PathOperator {
  override def serialize(implicit prefixes: Prefixes) = "[" + property + " " + operator + " " + value + "]"
}
