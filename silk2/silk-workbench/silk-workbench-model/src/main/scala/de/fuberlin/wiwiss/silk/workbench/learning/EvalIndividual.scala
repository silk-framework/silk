package de.fuberlin.wiwiss.silk.workbench.learning

import de.fuberlin.wiwiss.silk.learning.individual.Individual
import de.fuberlin.wiwiss.silk.evaluation.EvaluationResult

class EvalIndividual(individual: Individual, val scores: EvaluationResult) extends Individual(individual.node, individual.fitness)