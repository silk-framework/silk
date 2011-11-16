package de.fuberlin.wiwiss.silk.workbench.learning

import de.fuberlin.wiwiss.silk.workbench.workspace.TaskData

/**
 * Holds the current population sorter.
 */
object PopulationSorter extends TaskData[PopulationSorter](ScoreSorterDescending) {
  def sort(individuals: Seq[EvalIndividual]): Seq[EvalIndividual] = {
    apply()(individuals)
  }
}

/**
 * Sorts the individuals in the population.
 */
trait PopulationSorter extends (Seq[EvalIndividual] => Seq[EvalIndividual]) {
  def apply(individuals: Seq[EvalIndividual]): Seq[EvalIndividual]
}

object NoSorter extends PopulationSorter {
  def apply(individuals: Seq[EvalIndividual]) = individuals
}

object ScoreSorterAscending extends PopulationSorter {
  def apply(individuals: Seq[EvalIndividual]): Seq[EvalIndividual] = {
    individuals.sortBy(_.scores.score)
  }
}

object ScoreSorterDescending extends PopulationSorter {
  def apply(individuals: Seq[EvalIndividual]): Seq[EvalIndividual] = {
    individuals.sortBy(-_.scores.score)
  }
}

object MccSorterAscending extends PopulationSorter {
  def apply(individuals: Seq[EvalIndividual]): Seq[EvalIndividual] = {
    individuals.sortBy(_.scores.mcc)
  }
}

object MccSorterDescending extends PopulationSorter {
  def apply(individuals: Seq[EvalIndividual]): Seq[EvalIndividual] = {
    individuals.sortBy(-_.scores.mcc)
  }
}

object FMeasureSorterAscending extends PopulationSorter {
  def apply(individuals: Seq[EvalIndividual]): Seq[EvalIndividual] = {
    individuals.sortBy(_.scores.fMeasure)
  }
}

object FMeasureSorterDescending extends PopulationSorter {
  def apply(individuals: Seq[EvalIndividual]): Seq[EvalIndividual] = {
    individuals.sortBy(-_.scores.fMeasure)
  }
}