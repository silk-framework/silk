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

package de.fuberlin.wiwiss.silk.learning

import java.util.logging.Level
import de.fuberlin.wiwiss.silk.runtime.task.ValueTask
import de.fuberlin.wiwiss.silk.evaluation.LinkageRuleEvaluator
import de.fuberlin.wiwiss.silk.learning.LinkageRuleLearner.Result
import de.fuberlin.wiwiss.silk.learning.genlink.GenLink

/**
 * Learns a linkage rule from reference links.
 */
class LearningTask(input: LearningInput = LearningInput.empty,
                   config: LearningConfiguration = LearningConfiguration.default) extends ValueTask[LearningResult](LearningResult()) {

  private val learner = GenLink(config)

  /** The time when the learning task has been started. */
  @volatile private var startTime = 0L

  /** Set if the task has been stopped. */
  @volatile private var stop = false

  /** Don't log progress. */
  progressLogLevel = Level.FINE

  /** Returns the learning result. */
  def result = value.get

  /** Checks if this task is empty. */
  def isEmpty = input.trainingEntities.isEmpty

  /**
   * Executes this learning task.
   */
  override def execute(): LearningResult = {
    // Reset state
    startTime = System.currentTimeMillis
    stop = false

    // Execute linkage rule learner
    val learnerTask = learner.learn(input.trainingEntities, input.seedLinkageRules)
    learnerTask.onUpdate(updateStatus)
    learnerTask.value.onUpdate(updateValue)
    learnerTask()

    // Return the final value
    value.get
  }

  /**
   * Stops this learning task.
   */
  override def stopExecution() {
    stop = true
  }

  private def updateValue(value: Result): Unit = {
    val bestRule = value.population.bestIndividual.node.build
    // TODO build training and validation result lazily
    val result =
      LearningResult(
        iterations = value.iterations,
        time = System.currentTimeMillis() - startTime,
        population = value.population,
        trainingResult = LinkageRuleEvaluator(bestRule, input.trainingEntities),
        validationResult = LinkageRuleEvaluator(bestRule, input.validationEntities),
        status = value.message
      )
    LearningTask.this.value.update(result)
  }
}