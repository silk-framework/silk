/* 
 * Copyright 2009-2011 Freie Universität Berlin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.fuberlin.wiwiss.silk.learning.generation

import de.fuberlin.wiwiss.silk.util.ParallelMapper
import de.fuberlin.wiwiss.silk.evaluation.LinkageRuleEvaluator
import de.fuberlin.wiwiss.silk.util.task.Task
import util.Random
import de.fuberlin.wiwiss.silk.learning.individual.{LinkageRuleNode, Individual, Population}
import de.fuberlin.wiwiss.silk.learning.{LearningConfiguration, LearningInput}
import de.fuberlin.wiwiss.silk.linkagerule.{input, LinkageRule}

/**
 * Generates a new population of linkage rules.
 */
class GeneratePopulationTask(seedLinkageRules: Traversable[LinkageRule], generator: LinkageRuleGenerator, config: LearningConfiguration) extends Task[Population] {

  override def execute(): Population = {
    val individuals = new ParallelMapper(0 until config.parameters.populationSize).map { i =>
      updateStatus(i.toDouble / config.parameters.populationSize);
      generateIndividual()
    }

    Population(individuals)
  }

  private def generateIndividual(): Individual = {
    val linkageRule = generateRule()
    Individual(linkageRule, 0.0)
  }

  private def generateRule() = {
    if(!seedLinkageRules.isEmpty && Random.nextDouble() < 0.1)
      LinkageRuleNode.load(seedLinkageRules.toSeq(Random.nextInt(seedLinkageRules.size)))
    else
      generator()
  }
}
