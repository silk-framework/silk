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

package de.fuberlin.wiwiss.silk.workspace.scripts

import de.fuberlin.wiwiss.silk.workspace.scripts.RunResult.Run
import de.fuberlin.wiwiss.silk.learning.LearningResult

/**
 * Holds the result of a sequence of learning runs.
 */
case class RunResult(runs: Seq[Run]) {
  override def toString = runs.mkString("\n")
}

object RunResult {
  /**
   * Holds the results of a single learning run.
   */
  case class Run(results: Seq[LearningResult]) {
    override def toString = results.mkString(", ")
    /**
     * Compute the number of iterations needed to reach a specific F-measure.
     */
    def iterations(fMeasure: Double): Int = {
      results.indexWhere(_.validationResult.fMeasure >= fMeasure) match {
        case -1 => 50//throw new IllegalArgumentException("Target F-measure " + fMeasure + " never reached.")
        case i => i
      }
    }
  }
}