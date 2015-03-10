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
import de.fuberlin.wiwiss.silk.runtime.activity.{ActivityContext, Activity}
import de.fuberlin.wiwiss.silk.evaluation.LinkageRuleEvaluator
import de.fuberlin.wiwiss.silk.learning.LinkageRuleLearner.Result
import de.fuberlin.wiwiss.silk.learning.genlink.GenLinkLearner

/**
 * Learns a linkage rule from reference links.
 */
class LearningActivity(input: LearningInput = LearningInput.empty,
                       config: LearningConfiguration = LearningConfiguration.default) extends Activity[LearningResult] {

  private val learner = GenLinkLearner(config)

  /** The time when the learning task has been started. */
  @volatile private var startTime = 0L

  /** Set if the task has been stopped. */
  @volatile private var stop = false

  /** Checks if this task is empty. */
  def isEmpty = input.trainingEntities.isEmpty
  
  override def initialValue = LearningResult()

  /**
   * Executes this learning task.
   */
  override def run(context: ActivityContext[LearningResult]): Unit = {
    // Reset state
    startTime = System.currentTimeMillis
    stop = false

    // Execute linkage rule learner
    val learnerActivity = learner.learn(input.trainingEntities, input.seedLinkageRules)
    context.executeBlocking(learnerActivity, 1.0, updateValue(context))
  }

  /**
   * Stops this learning task.
   */
  override def cancelExecution() {
    stop = true
  }

  private def updateValue(context: ActivityContext[LearningResult])(value: Result): Unit = {
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
    context.value.update(result)
  }
}