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

package de.fuberlin.wiwiss.silk.learning

import de.fuberlin.wiwiss.silk.evaluation.ReferenceEntities
import de.fuberlin.wiwiss.silk.linkagerule.LinkageRule

/**
 * The input of the learning algorithm.
 *
 * @param trainingEntities Reference entities used for training
 * @param validationEntities Reference entities used for validation
 * @param seedLinkageRules Existing linkage rules which are used to seed the population.
 */
case class LearningInput(trainingEntities: ReferenceEntities = ReferenceEntities.empty,
                         validationEntities: ReferenceEntities = ReferenceEntities.empty,
                         seedLinkageRules: Traversable[LinkageRule] = Traversable.empty)

object LearningInput {
  def empty = LearningInput()
}