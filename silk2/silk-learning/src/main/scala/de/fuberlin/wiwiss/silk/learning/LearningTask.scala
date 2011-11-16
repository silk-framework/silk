package de.fuberlin.wiwiss.silk.learning

import cleaning.CleanPopulationTask
import generation.{LinkageRuleGenerator, GeneratePopulationTask}
import individual.Population
import java.util.logging.Level
import reproduction.ReproductionTask
import de.fuberlin.wiwiss.silk.util.task.{Task, ValueTask}
import de.fuberlin.wiwiss.silk.evaluation.LinkageRuleEvaluator
import de.fuberlin.wiwiss.silk.linkagerule.LinkageRule

/**
 * Learns a linkage rule from reference links.
 */
class LearningTask(input: LearningInput = LearningInput.empty,
                   config: LearningConfiguration = LearningConfiguration.load()) extends ValueTask[LearningResult](LearningResult()) {

  /** Maximum difference between two fitness values to be considered equal. */
  private val scoreEpsilon = 0.0001

  @volatile private var startTime = 0L

  /** Set if the task has been stopped. */
  @volatile private var stop = false

  @volatile private var ineffectiveIterations = 0

  /** Don't log progress. */
  progressLogLevel = Level.FINE

  def result = value.get

  override def execute(): LearningResult = {
    //Reset state
    startTime = System.currentTimeMillis
    stop = false
    ineffectiveIterations = 0

    val referenceEntities = input.trainingEntities
    val fitnessFunction = (rule: LinkageRule) => LinkageRuleEvaluator(rule, referenceEntities).score
    val generator = LinkageRuleGenerator(referenceEntities, config.components)

    //Generate initial population
    if(!stop) executeTask(new GeneratePopulationTask(input.seedLinkageRules, generator, config))

    while (!stop && !value.get.status.isInstanceOf[LearningResult.Finished]) {
      executeTask(new ReproductionTask(value.get.population, fitnessFunction, generator, config))

      if (value.get.iterations % config.parameters.cleanFrequency == 0 && !stop) {
        executeTask(new CleanPopulationTask(value.get.population, fitnessFunction, generator))
      }
    }

    if(!value.get.population.isEmpty)
      executeTask(new CleanPopulationTask(value.get.population, fitnessFunction, generator))

    value.get
  }

  override def stopExecution() {
    stop = true
  }

  private def executeTask(task: Task[Population]) {
    updateStatus(0.0)
    val population = executeSubTask(task)
    val iterations = {
      if(task.isInstanceOf[ReproductionTask]) {
        if (population.bestIndividual.fitness <= bestScore + scoreEpsilon)
          ineffectiveIterations += 1

        value.get.iterations + 1
      } else {
        value.get.iterations
      }
    }

    val status =
      if (population.bestIndividual.fitness > config.parameters.destinationfMeasure)
        LearningResult.Success
      else if (ineffectiveIterations >= config.parameters.maxIneffectiveIterations)
        LearningResult.MaximumIneffectiveIterationsReached
      else if (iterations >= config.parameters.maxIterations)
        LearningResult.MaximumIterationsReached
      else
        LearningResult.Running

    val result =
      LearningResult(
        iterations = iterations,
        time = System.currentTimeMillis() - startTime,
        population = population,
        validationResult = LinkageRuleEvaluator(population.bestIndividual.node.build, input.trainingEntities), //TODO should this be validation Entities?
        status = status
      )

    value.update(result)
  }

  private def bestScore = {
    if(value.get.population.isEmpty)
      Double.NegativeInfinity
    else
      value.get.population.bestIndividual.fitness
  }
}
