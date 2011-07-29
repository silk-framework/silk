package de.fuberlin.wiwiss.silk.learning

import cleaning.CleanPopulationTask
import de.fuberlin.wiwiss.silk.evaluation.ReferenceInstances
import generation.GeneratePopulationTask
import individual.Population
import java.util.logging.Level
import de.fuberlin.wiwiss.silk.util.task.ValueTask
import reproduction.ReproductionTask

class LearningTask(instances: ReferenceInstances) extends ValueTask[Population](Population()) {

  private val config = LearningConfiguration.load(instances)

  /** The desired fMeasure. The algorithm will stop after reaching it. */
  private val destinationfMeasure = 0.999

  private val cleanFrequency = 5

  /** The maximum number of iterations before giving up. */
  private val maxIterations = 5000

  /** The maximum number of subsequent iterations without any increase in fitness before giving up. */
  private val maxIneffectiveIterations = 500

  /** Maximum difference between two fitness values to be considered equal. */
  private val fitnessEpsilon = 0.0001

  @volatile private var stop = false

  @volatile var statistics: LearningStatistics = null

  logLevel = Level.FINE

  override def execute(): Population = {
    stop = false
    val startTime = System.currentTimeMillis

    updateStatus(0.0)
    value.update(executeSubTask(new GeneratePopulationTask(instances, config)))

    var bestMeasure = 0.0
    var ineffectiveIterations = 0
    var iteration = 0
    var message = ""

    while (!stop) {
      iteration += 1

      println("Iteration " + iteration)

      updateStatus(0.0)
      value.update(executeSubTask(new ReproductionTask(value.get, instances, config)))

      if (iteration % cleanFrequency == 0) {
        println("Cleaning")
        updateStatus(0.0)
        value.update(executeSubTask(new CleanPopulationTask(value.get, instances, config)))
      }

      val fMeasure = value.get.individuals.map(_.fitness.fMeasure).max

      if (fMeasure > destinationfMeasure) {
        message = "Success"
        stop = true
      } else if (fMeasure <= bestMeasure + fitnessEpsilon) {
        ineffectiveIterations += 1
        if(ineffectiveIterations > maxIneffectiveIterations) {
          message = "Too many ineffective iterations"
          stop = true
        }
      }

      if (iteration >= maxIterations) {
        message = "Reached maximum iterations"
        stop = true
      }

      bestMeasure = fMeasure
    }

    updateStatus(0.0)
    value.update(executeSubTask(new CleanPopulationTask(value.get, instances, config)))

    statistics = LearningStatistics(System.currentTimeMillis() - startTime, iteration, message)
    logger.info(statistics.toString)

    value.get
  }

  override def stopExecution() {
    stop = true
  }
}
