package de.fuberlin.wiwiss.silk.learning

import cleaning.CleanPopulationTask
import generation.GeneratePopulationTask
import individual.Population
import java.util.logging.Level
import reproduction.ReproductionTask
import de.fuberlin.wiwiss.silk.util.task.{Task, ValueTask}
import de.fuberlin.wiwiss.silk.evaluation.{LinkConditionEvaluator, ReferenceInstances}

class LearningTask(instances: ReferenceInstances,
                   validationInstances: ReferenceInstances = ReferenceInstances.empty) extends ValueTask[LearningResult](LearningResult()) {

  /** The learning configuration. */
  private val config = LearningConfiguration.load(instances)

  /** The desired fMeasure. The algorithm will stop after reaching it. */
  private val destinationfMeasure = 0.999

  private val cleanFrequency = 5

  /** The maximum number of iterations before giving up. */
  private val maxIterations = 50

  /** The maximum number of subsequent iterations without any increase in fitness before giving up. */
  private val maxIneffectiveIterations = 50

  /** Maximum difference between two fitness values to be considered equal. */
  private val scoreEpsilon = 0.0001

  @volatile private var startTime = 0L

  /** Set if the task has been stopped. */
  @volatile private var stop = false

  @volatile private var ineffectiveIterations = 0

  /** Don't log progress. */
  logLevel = Level.FINE

  override def execute(): LearningResult = {
    //Reset state
    startTime = System.currentTimeMillis
    stop = false
    ineffectiveIterations = 0

    //Generate initial population
    executeTask(new GeneratePopulationTask(instances, config))

    while (!stop) {
      executeTask(new ReproductionTask(value.get.population, instances, config))

      if (value.get.iterations % cleanFrequency == 0) {
        executeTask(new CleanPopulationTask(value.get.population, instances, config))
      }

      stop = value.get.status.isInstanceOf[LearningResult.Finished]
    }

    executeTask(new CleanPopulationTask(value.get.population, instances, config))

    value.get
  }

  override def stopExecution() {
    stop = true
  }

  private def executeTask(task: Task[Population]) {
    updateStatus(0.0)
    val population = executeSubTask(task)
    val iterations = if(task.isInstanceOf[ReproductionTask]) value.get.iterations + 1 else value.get.iterations

    if (population.bestIndividual.fitness.score <= bestScore + scoreEpsilon)
      ineffectiveIterations += 1

    val status =
      if (population.bestIndividual.fitness.fMeasure > destinationfMeasure)
        LearningResult.Success
      else if (ineffectiveIterations >= maxIneffectiveIterations)
          LearningResult.MaximumIneffectiveIterationsReached
      else if (iterations >= maxIterations)
        LearningResult.MaximumIterationsReached
      else
        LearningResult.Running

    val result =
      LearningResult(
        iterations = iterations,
        time = System.currentTimeMillis() - startTime,
        population = population,
        validationResult = LinkConditionEvaluator(population.bestIndividual.node.build, validationInstances),
        status = status
      )

    value.update(result)
  }

  private def bestScore = {
    if(value.get.population.isEmpty)
      Double.NegativeInfinity
    else
      value.get.population.bestIndividual.fitness.score
  }
}
