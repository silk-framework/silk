package de.fuberlin.wiwiss.silk.linkspec.path

/**
 * Represents an operator in an RDF path.
 */
sealed abstract class PathOperator

/**
 * Moves forward from a subject resource (set) through a property to its object resource (set).
 */
case class ForwardOperator(property : String) extends PathOperator

/**
 * Moves backward from an object resource (set) through a property to its subject resource (set).
 */
case class BackwardOperator(property : String) extends PathOperator

/**
 * Reduces the currently selected set of resources to the ones with a specific language.
 *
 * @param operator Comparison operator. May be one of >, <, >=, <=, =, !=.
 * @param value The language.
 */
case class LanguageFilter(operator : String, language : String) extends PathOperator

/**
 * Reduces the currently selected set of resources to the ones matching the filter expression.
 *
 * @param property The property which has the values which should be use for filtering
 * @param operator Comparison operator. May be one of >, <, >=, <=, =, !=.
 * @param value The comparison value.
 */
case class PropertyFilter(property : String, operator : String, value : String) extends PathOperator
