package de.fuberlin.wiwiss.silk.learning

import individual.Individual

/**
 * Represents a population of candidate solutions.
 */
case class Population(individuals : Traversable[Individual] = Traversable.empty)