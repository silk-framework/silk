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

package org.silkframework.learning.active

import org.silkframework.learning.individual.Population
import org.silkframework.util.DPair._
import org.silkframework.rule.{Operator, LinkageRule}
import org.silkframework.rule.similarity.{Comparison, Aggregation}
import org.silkframework.rule.input.{PathInput, TransformInput}
import org.silkframework.entity.{Link, Path}
import org.silkframework.util.DPair

//TODO not used at the moment
private object FilterPopulation {
  def apply(population: Population, links: Traversable[Link]) = {
    Population(population.individuals.filter(i => rateRule(i.node.build, links)))
  }

  private def rateRule(rule: LinkageRule, links: Traversable[Link]): Boolean = {
    val entityPairs = links.toSeq.map(_.entities.get)
    val shuffledEntityPairs = for((s, t) <- entityPairs.map(_.source) zip (entityPairs.tail.map(_.target) :+ entityPairs.head.target)) yield DPair(s, t)

    val count = (entityPairs ++ shuffledEntityPairs).filter(rule(_) > 0).size

    println(formatRule(rule) + ": " + count + "/" + links.size)

    count > 0 && count <= links.size
  }

  private def formatRule(rule: LinkageRule) = {
    def collectPaths(op: Operator): Seq[Path] = op match {
      case agg: Aggregation => agg.operators.flatMap(collectPaths)
      case cmp: Comparison => cmp.inputs.flatMap(collectPaths)
      case t: TransformInput => t.inputs.flatMap(collectPaths)
      case i: PathInput => Seq(i.path)
    }

    val paths = collectPaths(rule.operator.get)
    val shortPaths  = paths.map(_.serialize.split("[/#]").last.init)

    shortPaths.mkString(" ")
  }
}