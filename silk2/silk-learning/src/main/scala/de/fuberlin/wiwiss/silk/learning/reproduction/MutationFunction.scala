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

package de.fuberlin.wiwiss.silk.learning.reproduction

import de.fuberlin.wiwiss.silk.learning.generation.LinkageRuleGenerator
import de.fuberlin.wiwiss.silk.learning.individual.{Individual, LinkageRuleNode}

/**
 * Mutates a linkage rule.
 */
class MutationFunction(crossover: CrossoverFunction,
                       generator: LinkageRuleGenerator) extends (Individual => Individual) {

  def apply(ind: Individual): Individual = {
    crossover(ind, Individual(generator(), 0.0))
  }
}