package de.fuberlin.wiwiss.silk.learning.individual

/**
 * Represents a population of candidate solutions.
 */
case class Population(individuals : Traversable[Individual] = Traversable.empty) {
  lazy val bestIndividual = individuals.maxBy(_.fitness.score)

  def isEmpty = individuals.isEmpty
}