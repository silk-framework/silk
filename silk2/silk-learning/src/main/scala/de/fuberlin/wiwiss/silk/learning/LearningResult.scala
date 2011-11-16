package de.fuberlin.wiwiss.silk.learning

import individual.Population
import de.fuberlin.wiwiss.silk.evaluation.EvaluationResult
import de.fuberlin.wiwiss.silk.learning.LearningResult._
import de.fuberlin.wiwiss.silk.linkagerule.LinkageRule

case class LearningResult(iterations: Int = 0,
                          time: Long = 0,
                          population: Population = Population(),
                          trainingResult: EvaluationResult =  new EvaluationResult(0, 0, 0, 0),
                          validationResult: EvaluationResult = new EvaluationResult(0, 0, 0, 0),
                          status: Status = NotStarted) {

  def linkageRule = population.bestIndividual.node.build
}

object LearningResult {
  sealed trait Status

  case object NotStarted extends Status

  case object Running extends Status

  trait Finished extends Status

  case object MaximumIterationsReached extends Finished

  case object MaximumIneffectiveIterationsReached extends Finished

  case object Success extends Finished
}

object LearningResultLatexFormatter {
  def apply(statistics: Iterable[LearningResult]): String = {
    val header =
      "\\begin{tabular}{| l | l | l | l |}\n" +
      "\\hline\n" +
      "Run & Iterations & Time & Result \\\\\n" +
      "\\hline\n"

    val rows = statistics.zipWithIndex.map(row _ tupled).mkString

    val footer =
      "\\hline\n" +
 	    "\\end{tabular}\n"

    header + rows + averageRow(statistics) + footer
  }

  private def row(statistics: LearningResult, run: Int): String = {
    run + " & " + statistics.iterations + " & " + (statistics.time / 1000.0) + "s & " + statistics.status.toString + "\\\\\n"
  }

  private def averageRow(statistics: Iterable[LearningResult]): String = {
    val averageIterations = statistics.map(_.iterations).sum.toDouble / statistics.size
    val averageTime = statistics.map(_.time / 1000.0).sum / statistics.size

    "\\hline\n" +
    "Average & " + averageIterations + " & " + averageTime + "s & \\\\\n"
  }
}