package de.fuberlin.wiwiss.learning.individual

import de.fuberlin.wiwiss.silk.evaluation.EvaluationResult
import de.fuberlin.wiwiss.silk.workbench.learning.tree.LinkConditionNode

case class Individual(node : LinkConditionNode, fitness : EvaluationResult)