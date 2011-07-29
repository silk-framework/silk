package de.fuberlin.wiwiss.silk.workbench.learning

import de.fuberlin.wiwiss.silk.workbench.workspace.UserData
import de.fuberlin.wiwiss.silk.learning.individual.Individual

/**
 * Holds the current population sorter.
 */
object PopulationSorter extends UserData[PopulationSorter](ScoreSorterDescending) {
  def sort(individuals: Seq[Individual]): Seq[Individual] = {
    apply()(individuals)
  }
}

/**
 * Sorts the individuals in the population.
 */
trait PopulationSorter extends (Seq[Individual] => Seq[Individual]) {
  def apply(individuals: Seq[Individual]): Seq[Individual]
}

object NoSorter extends PopulationSorter {
  def apply(individuals: Seq[Individual]) = individuals
}

object ScoreSorterAscending extends PopulationSorter {
  def apply(individuals: Seq[Individual]): Seq[Individual] = {
    individuals.sortBy(_.fitness.score)
  }
}

object ScoreSorterDescending extends PopulationSorter {
  def apply(individuals: Seq[Individual]): Seq[Individual] = {
    individuals.sortBy(-_.fitness.score)
  }
}

object MccSorterAscending extends PopulationSorter {
  def apply(individuals: Seq[Individual]): Seq[Individual] = {
    individuals.sortBy(_.fitness.mcc)
  }
}

object MccSorterDescending extends PopulationSorter {
  def apply(individuals: Seq[Individual]): Seq[Individual] = {
    individuals.sortBy(-_.fitness.mcc)
  }
}

object FMeasureSorterAscending extends PopulationSorter {
  def apply(individuals: Seq[Individual]): Seq[Individual] = {
    individuals.sortBy(_.fitness.fMeasure)
  }
}

object FMeasureSorterDescending extends PopulationSorter {
  def apply(individuals: Seq[Individual]): Seq[Individual] = {
    individuals.sortBy(-_.fitness.fMeasure)
  }
}