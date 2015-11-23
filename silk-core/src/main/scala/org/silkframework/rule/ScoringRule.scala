package org.silkframework.rule

import org.silkframework.rule.input.Input
import org.silkframework.util.{Uri, Identifier}

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
