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

package de.fuberlin.wiwiss.silk.learning.individual

/**
 * Represents a population of candidate solutions.
 */
case class Population(individuals : Traversable[Individual] = Traversable.empty) {

  /** The individual with the best score */
  lazy val bestIndividual = individuals.maxBy(_.fitness)

  /** True, if the population is empty */
  def isEmpty = individuals.isEmpty
}