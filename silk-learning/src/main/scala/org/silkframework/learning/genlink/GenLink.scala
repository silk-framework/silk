package org.silkframework.learning.genlink

import org.silkframework.learning.LearningConfiguration
import org.silkframework.learning.LinkageRuleLearner.Result
import org.silkframework.learning.cleaning.CleanPopulationTask
import org.silkframework.learning.generation.{GeneratePopulation, LinkageRuleGenerator}
import org.silkframework.learning.individual.Population
import org.silkframework.learning.reproduction.Reproduction
import org.silkframework.rule.LinkageRule
import org.silkframework.rule.evaluation.{LinkageRuleEvaluator, ReferenceEntities}
import org.silkframework.runtime.activity.{Activity, ActivityContext, UserContext}
import org.silkframework.runtime.plugin.PluginContext

import scala.util.Random

private class GenLink(trainingLinks: ReferenceEntities, seeds: Traversable[LinkageRule],
                      config: LearningConfiguration) extends Activity[Result] {

  /** Maximum difference between two fitness values to be considered equal. */
  private val scoreEpsilon = 0.0001

  /** The current linkage rule population. */
  @volatile private var population: Population = _

  /** The number of iterations. */
  @volatile private var iterations = 0

  /** The status of this task. */
  @volatile private var learningStatus: Status.Status = _

  /** The number of ineffective iterations, i.e., iterations that did not improve the fitness significantly. */
  @volatile private var ineffectiveIterations = 0
  
  override def initialValue = Some(Result(Population.empty, 0, ""))

  override def run(context: ActivityContext[Result])
                  (implicit userContext: UserContext): Unit = {
    //Reset state
    cancelled = false
    iterations = 0
    learningStatus = Status.NotStarted
    ineffectiveIterations = 0

    implicit val pluginContext: PluginContext = PluginContext.empty

    val fitnessFunction = config.fitnessFunction(trainingLinks)
    val generator = LinkageRuleGenerator(trainingLinks, config.components)
    val random = Random

    //Generate initial population
    if(!cancelled) executeStep(new GeneratePopulation(seeds, generator, config, random.nextLong()), context)

    while (!cancelled && !learningStatus.isInstanceOf[Status.Finished]) {
      executeStep(new Reproduction(population, fitnessFunction, generator, config, random.nextLong()), context)

      if (iterations % config.params.cleanFrequency == 0 && !cancelled) {
        executeStep(new CleanPopulationTask(population, fitnessFunction, generator, random.nextLong()), context)
      }
    }

    if(!population.isEmpty)
      executeStep(new CleanPopulationTask(population, fitnessFunction, generator, random.nextLong()), context)

    Result(population, iterations, learningStatus.toString)
  }

  private def executeStep(activity: Activity[Population], context: ActivityContext[Result])
                         (implicit userContext: UserContext){
    // Update population
    population = context.child(activity).startBlockingAndGetValue()

    // Update number of iterations
    if(activity.isInstanceOf[Reproduction]) {
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
    context.value.update(Result(population, iterations, learningStatus.toString))
    context.status.updateProgress(iterations.toDouble / config.params.maxIterations)
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