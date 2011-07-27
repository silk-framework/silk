package de.fuberlin.wiwiss.silk.learning

import de.fuberlin.wiwiss.silk.util.task.Task
import de.fuberlin.wiwiss.silk.evaluation.ReferenceInstances
import java.util.logging.Level

class ExperimentTask(instances : ReferenceInstances) extends Task[Unit]
{
  private val numRuns = 10

  logLevel = Level.FINE

  override def execute()
  {
    val statistics =
      for(run <- 1 to numRuns) yield
      {
        val learningTask = new LearningTask(instances)
        executeSubTask(learningTask, run.toDouble / numRuns)
        learningTask.statistics
      }

    println(StatisticsLatexFormatter(statistics))
  }
}