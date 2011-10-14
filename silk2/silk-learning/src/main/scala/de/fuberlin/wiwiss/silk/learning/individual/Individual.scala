package de.fuberlin.wiwiss.silk.learning.individual

import de.fuberlin.wiwiss.silk.evaluation.EvaluationResult

/**
 * An individual in the population.
 */
case class Individual(node : LinkageRuleNode, fitness : EvaluationResult)