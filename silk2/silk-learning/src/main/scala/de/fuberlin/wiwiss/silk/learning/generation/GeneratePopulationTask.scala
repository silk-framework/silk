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

package de.fuberlin.wiwiss.silk.learning.generation

import de.fuberlin.wiwiss.silk.runtime.task.Task
import util.Random
import de.fuberlin.wiwiss.silk.learning.individual.{LinkageRuleNode, Individual, Population}
import de.fuberlin.wiwiss.silk.learning.{LearningConfiguration, LearningInput}
import de.fuberlin.wiwiss.silk.linkagerule.{input, LinkageRule}

/**
 * Generates a new population of linkage rules.
 */
class GeneratePopulationTask(seedLinkageRules: Traversable[LinkageRule], generator: LinkageRuleGenerator, config: LearningConfiguration) extends Task[Population] {

  override def execute(): Population = {
    val individuals = for(i <- (0 until config.params.populationSize).par) yield {
      updateStatus(i.toDouble / config.params.populationSize)
      generateIndividual()
    }

    Population(individuals.seq)
  }

  private def generateIndividual(): Individual = {
    val linkageRule = generateRule()
    Individual(linkageRule, 0.0)
  }

  private def generateRule() = {
    val nonEmptyRules = seedLinkageRules.filter(_.operator.isDefined).toIndexedSeq
    if(!nonEmptyRules.isEmpty && Random.nextDouble() < 0.1)
      LinkageRuleNode.load(nonEmptyRules(Random.nextInt(nonEmptyRules.size)))
    else
      generator()
  }
}
