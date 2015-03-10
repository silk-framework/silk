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

import individual.Population
import de.fuberlin.wiwiss.silk.evaluation.EvaluationResult

case class LearningResult(iterations: Int = 0,
                          time: Long = 0,
                          population: Population = Population.empty,
                          trainingResult: EvaluationResult =  new EvaluationResult(0, 0, 0, 0),
                          validationResult: EvaluationResult = new EvaluationResult(0, 0, 0, 0),
                          status: String = "") {

  def linkageRule = population.bestIndividual.node.build
}

object LearningResultLatexFormatter {
  def apply(statistics: Iterable[LearningResult]): String = {
    val header =
      "\\begin{tabular}{| l | l | l | l |}\n" +
      "\\hline\n" +
      "Run & Iterations & Time & Result \\\\\n" +
      "\\hline\n"

    val rows = statistics.zipWithIndex.map(row _ tupled).mkString

    val footer =
      "\\hline\n" +
 	    "\\end{tabular}\n"

    header + rows + averageRow(statistics) + footer
  }

  private def row(statistics: LearningResult, run: Int): String = {
    run + " & " + statistics.iterations + " & " + (statistics.time / 1000.0) + "s & " + statistics.status.toString + "\\\\\n"
  }

  private def averageRow(statistics: Iterable[LearningResult]): String = {
    val averageIterations = statistics.map(_.iterations).sum.toDouble / statistics.size
    val averageTime = statistics.map(_.time / 1000.0).sum / statistics.size

    "\\hline\n" +
    "Average & " + averageIterations + " & " + averageTime + "s & \\\\\n"
  }
}