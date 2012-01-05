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

import de.fuberlin.wiwiss.silk.workbench.scripts.RunResult.Run

case class RunResult(runs: Seq[Run]) {
  override def toString = runs.mkString("\n")
}

object RunResult {

  //TODO delete?
  def mergeRuns(runs: Seq[Run]) = {
    val maxIterations = runs.map(_.results.size).max
    val fMeasures = runs.map(_.results.padTo(maxIterations, 1.0))
    val meanfMeasures = fMeasures.transpose.map(d => d.sum / d.size)
    Run(meanfMeasures)
  }
  
  case class Run(results: Seq[Double]) {
    override def toString = results.mkString(", ")
    /**
     * Compute the number of iterations needed to reach a specific F-measure.
     */
    def iterations(fMeasure: Double): Int = {
      results.indexWhere(_ >= fMeasure) match {
        case -1 => 50//throw new IllegalArgumentException("Target F-measure " + fMeasure + " never reached.")
        case i => i
      }
    }
  }
}