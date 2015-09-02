package de.fuberlin.wiwiss.silk.rule

import de.fuberlin.wiwiss.silk.rule.input.Input
import de.fuberlin.wiwiss.silk.util.{Uri, Identifier}

case class ScoringRule(name: Identifier = "score", operator: Input, target: Uri) {

}
