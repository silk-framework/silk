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

import de.fuberlin.wiwiss.silk.evaluation.statistics.AggregatedComplexity._
import de.fuberlin.wiwiss.silk.evaluation.statistics.LinkageRuleComplexity._
import de.fuberlin.wiwiss.silk.evaluation.statistics.{LinkageRuleComplexity, AggregatedComplexity, VariableStatistic}

trait PerformanceMetric extends (RunResult => Any) {
  def name: String
}

object PerformanceMetric {
  /**
   * Computes the validation F-measure after a fixed number of iterations.
   */
  case class FixedIterationsFMeasure(round: Int = 0) extends PerformanceMetric  {
    val name = round match {
      case 0 => "Initial F-measure"
      case _ => "F-measure in round " + round
    }

    def apply(result: RunResult) = {
      VariableStatistic(result.runs.map(_.results(round).validationResult.fMeasure))
    }
  }

  /**
   * Computes the rule size after a fixed number of iterations.
   */
  case class Size(round: Int = 0) extends PerformanceMetric  {
    val name = round match {
      case 0 => "Initial comparisons"
      case _ => "Comparisons in round " + round
    }

    def apply(result: RunResult) = {
      val rules = result.runs.map(_.results(round).linkageRule)
      val sizes = rules.map(LinkageRuleComplexity(_).size.toDouble)
      VariableStatistic(sizes).mean
    }
  }

  /**
   * Computes the mean iterations needed to reach a specific F-measure.
   */
  case class MeanIterations(targetFMeasure: Double = 0.999) extends PerformanceMetric {
    val name = "Mean iterations for " + (targetFMeasure * 100.0 + 0.5).toInt + "% F-measure"

    def apply(result: RunResult) = {
      result.runs.map(_.iterations(targetFMeasure)).sum.toDouble / result.runs.size
    } 
  }
}