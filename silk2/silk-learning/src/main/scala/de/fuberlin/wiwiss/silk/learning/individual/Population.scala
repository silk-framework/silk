package de.fuberlin.wiwiss.silk.learning.individual

/**
 * Represents a population of candidate solutions.
 */
case class Population(individuals : Traversable[Individual] = Traversable.empty) {

  /** The individual with the best score */
  //TODO also sort by size (smallest first)
  lazy val bestIndividual = individuals.maxBy(_.fitness)

  /** True, if the population is empty */
  def isEmpty = individuals.isEmpty
}