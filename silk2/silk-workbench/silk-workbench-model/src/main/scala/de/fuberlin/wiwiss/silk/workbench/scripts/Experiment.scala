/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.fuberlin.wiwiss.silk.workbench.scripts

import de.fuberlin.wiwiss.silk.learning.LearningConfiguration
import de.fuberlin.wiwiss.silk.learning.LearningConfiguration.{Parameters, Components}
import de.fuberlin.wiwiss.silk.learning.individual.fitness.{FMeasureFitness, MCCFitnessFunction}
import de.fuberlin.wiwiss.silk.workbench.scripts.PerformanceMetric.{Size, FixedIterationsFMeasure}

/**
 * An experiment consisting of a number of configurations which should be compared and a number of performance metrics.
 */
case class Experiment(name: String, configurations: Seq[LearningConfiguration], metrics: Seq[PerformanceMetric])

/**
 * All default experiments.
 */
object Experiment {
  /**
   * Runs the default configuration.
   */
  val default =
    Experiment("Default",
      configurations = LearningConfiguration("Default") :: Nil,
      metrics = FixedIterationsFMeasure(3) :: Nil
    )

  /**
   * Evaluates different linkage rule representations.
   */
  val representations =
    Experiment("representations",
      configurations =
        LearningConfiguration("Boolean",   components = Components(linear = false, boolean = true,  hierarchies = true,  transformations = false), params = Parameters(maxIterations = 25)) ::
        LearningConfiguration("Linear",    components = Components(linear = true,  boolean = false, hierarchies = false, transformations = false), params = Parameters(maxIterations = 25)) ::
        LearningConfiguration("Nonlinear", components = Components(linear = true,  boolean = true,  hierarchies = true,  transformations = false), params = Parameters(maxIterations = 25)) ::
        LearningConfiguration("Full",      components = Components(linear = true,  boolean = true,  hierarchies = true,  transformations = true),  params = Parameters(maxIterations = 25)) :: Nil,
      metrics =
          FixedIterationsFMeasure(0) :: FixedIterationsFMeasure(10) :: FixedIterationsFMeasure(25) :: Nil
    )

  /**
   * Compares different seeding strategies.
   */
  val seeding =
    Experiment("Seeding",
      configurations =
        LearningConfiguration("Random",       components = Components(seed = false), params = Parameters(maxIterations = 10)) ::
        LearningConfiguration("Our Approach", components = Components(seed = true),  params = Parameters(maxIterations = 10)) :: Nil,
      metrics =
        FixedIterationsFMeasure(0) :: FixedIterationsFMeasure(10) :: Nil
    )

  /**
   * Evaluates the contribution of using specialized crossover operators over using subtree crossover.
   */
  val crossover =
    Experiment("Crossover",
      configurations =
        LearningConfiguration("Subtree C.", components = Components(useSpecializedCrossover = false), params = Parameters(maxIterations = 50)) ::
        LearningConfiguration("Our Approach",      components = Components(useSpecializedCrossover = true),  params = Parameters(maxIterations = 50)) :: Nil,
      metrics =
        FixedIterationsFMeasure(10) :: FixedIterationsFMeasure(25) :: FixedIterationsFMeasure(50) :: Nil
    )

  /**
   * Compares different fitness functions.
   */
  val fitness =
    Experiment("Fitness functions",
      configurations =
        LearningConfiguration("F-measure", fitnessFunction = MCCFitnessFunction(0.0), params = Parameters(maxIterations = 10)) ::
        LearningConfiguration("MCC",       fitnessFunction = FMeasureFitness(),       params = Parameters(maxIterations = 10)) :: Nil,
      metrics =
        FixedIterationsFMeasure(10) :: Nil
    )

  /**
   * Compares different strategies for bloating control.
   */
  val bloating =
    Experiment("Bloating Control",
      configurations = LearningConfiguration("None",     params = Parameters(maxIterations = 10, cleanFrequency = Int.MaxValue), fitnessFunction = MCCFitnessFunction(0.0))   ::
                       LearningConfiguration("Penalty",  params = Parameters(maxIterations = 10, cleanFrequency = Int.MaxValue), fitnessFunction = MCCFitnessFunction(0.005)) ::
                       LearningConfiguration("Cleaning", params = Parameters(maxIterations = 10, cleanFrequency = 5),            fitnessFunction = MCCFitnessFunction(0.0))   ::
                       LearningConfiguration("Combined", params = Parameters(maxIterations = 10, cleanFrequency = 5),            fitnessFunction = MCCFitnessFunction(0.005)) :: Nil,
      metrics =
        FixedIterationsFMeasure(0) :: FixedIterationsFMeasure(10) :: Size(0) :: Size(10) :: Nil
    )
}