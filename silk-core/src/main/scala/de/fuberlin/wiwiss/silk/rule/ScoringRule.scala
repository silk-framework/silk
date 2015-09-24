package de.fuberlin.wiwiss.silk.rule

import de.fuberlin.wiwiss.silk.rule.input.Input
import de.fuberlin.wiwiss.silk.util.{Uri, Identifier}

case class ScoringRule(name: Identifier = "score", operator: Input, target: Uri) {

  def toTransform: TransformRule = {
    ComplexMapping(
      name = name,
      operator = operator,
      target = Some(target)
    )
  }
}

object ScoringRule {

  def fromTransform(rule: TransformRule) =
    ScoringRule(
      name = rule.name,
      operator = rule.operator,
      target = rule.target.getOrElse(throw new IllegalArgumentException("Cannot convert transform rule with empty target to scoring rule."))
    )

}
