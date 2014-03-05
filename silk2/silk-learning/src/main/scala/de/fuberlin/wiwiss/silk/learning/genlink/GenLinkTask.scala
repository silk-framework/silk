package de.fuberlin.wiwiss.silk.learning.genlink

import de.fuberlin.wiwiss.silk.evaluation.{LinkageRuleEvaluator, ReferenceEntities}
import de.fuberlin.wiwiss.silk.linkagerule.LinkageRule
import de.fuberlin.wiwiss.silk.learning.LearningConfiguration
import de.fuberlin.wiwiss.silk.runtime.task.{Task, ValueTask}
import de.fuberlin.wiwiss.silk.learning.LinkageRuleLearner.Result
import de.fuberlin.wiwiss.silk.learning.individual.Population
import de.fuberlin.wiwiss.silk.learning.generation.{GeneratePopulationTask, LinkageRuleGenerator}
import de.fuberlin.wiwiss.silk.learning.reproduction.ReproductionTask
import de.fuberlin.wiwiss.silk.learning.cleaning.CleanPopulationTask

private class GenLinkTask(trainingLinks: ReferenceEntities, seeds: Traversable[LinkageRule],
                          config: LearningConfiguration) extends ValueTask[Result](Result(Population.empty, 0, "")) {

  /** Maximum difference between two fitness values to be considered equal. */
  private val scoreEpsilon = 0.0001

  /** The current linkage rule population. */
  @volatile private var population: Population = _

  /** The number of iterations. */
  @volatile private var iterations = 0

  /** The status of this task. */
  @volatile private var learningStatus: Status.Status = _

  /** Set if the execution should be stopped. */
  @volatile private var stop = false

  /** The number of ineffective iterations, i.e., iterations that did not improve the fitness significantly. */
  @volatile private var ineffectiveIterations = 0

  override def execute(): Result = {
    //Reset state
    stop = false
    iterations = 0
    learningStatus = Status.NotStarted
    ineffectiveIterations = 0

    val fitnessFunction = config.fitnessFunction(trainingLinks)
    val generator = LinkageRuleGenerator(trainingLinks, config.components)

    //Generate initial population
    if(!stop) executeTask(new GeneratePopulationTask(seeds, generator, config))

    while (!stop && !learningStatus.isInstanceOf[Status.Finished]) {
      executeTask(new ReproductionTask(population, fitnessFunction, generator, config))

      if (iterations % config.params.cleanFrequency == 0 && !stop) {
        executeTask(new CleanPopulationTask(population, fitnessFunction, generator))
      }
    }

    if(!population.isEmpty)
      executeTask(new CleanPopulationTask(population, fitnessFunction, generator))

    Result(population, iterations, learningStatus.toString)
  }

  override def stopExecution() {
    stop = true
  }

  private def executeTask(task: Task[Population]) {
    // Update population
    population = task()

    // Update number of iterations
    if(task.isInstanceOf[ReproductionTask]) {
      if (population.bestIndividual.fitness <= bestScore + scoreEpsilon) {
        ineffectiveIterations += 1
      }
      iterations += 1
    }

    // Update status
    learningStatus =
        if (LinkageRuleEvaluator(population.bestIndividual.node.build, trainingLinks).fMeasure > config.params.destinationfMeasure)
          Status.Success
        else if (ineffectiveIterations >= config.params.maxIneffectiveIterations)
          Status.MaximumIneffectiveIterationsReached
        else if (iterations >= config.params.maxIterations)
          Status.MaximumIterationsReached
        else
          Status.Running

    // Report result
    value.update(Result(population, iterations, learningStatus.toString))
    updateStatus(iterations.toDouble / config.params.maxIterations)
  }

  /**
   * The fitness of the best individual in the population.
   */
  private def bestScore = {
    if(population.isEmpty)
      Double.NegativeInfinity
    else
      population.bestIndividual.fitness
  }

  /**
   * The statuses that this task may be into.
   */
  private object Status {

    sealed trait Status

    case object NotStarted extends Status

    case object Running extends Status

    trait Finished extends Status

    case object MaximumIterationsReached extends Finished

    case object MaximumIneffectiveIterationsReached extends Finished

    case object Success extends Finished
  }
}