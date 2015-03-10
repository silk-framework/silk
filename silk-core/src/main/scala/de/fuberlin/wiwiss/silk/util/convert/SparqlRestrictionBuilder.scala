package de.fuberlin.wiwiss.silk.util.convert

import de.fuberlin.wiwiss.silk.entity.{ForwardOperator, SparqlRestriction, Restriction}
import de.fuberlin.wiwiss.silk.entity.Restriction.{Operator, Condition, Or, And}
import de.fuberlin.wiwiss.silk.config.Prefixes

/**
 * Builds a SPARQL restriction from a Silk restriction.
 */
class SparqlRestrictionBuilder(subjectVar: String)(implicit prefixes: Prefixes) {

  def apply(restriction: Restriction): SparqlRestriction = {
    val sparql = restriction.operator.map(convertOperator).getOrElse("")
    SparqlRestriction.fromSparql("subjectVar", sparql)
  }

  def convertOperator(op: Operator): String = op match {
    case Condition(path, value) => path.operators match {
      case ForwardOperator(uri) :: Nil => s"{?$subjectVar <$uri> <$value>}"
      case _ => throw new UnsupportedOperationException("Complex paths are not supported.")
    }
    case And(ops) => ops.map(convertOperator).mkString("{", "\n", "}")
    case Or(ops) => ops.map(convertOperator).mkString("{", "\nUNION\n", "}")
  }
}
