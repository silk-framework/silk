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

trait PerformanceMetric extends (RunResult => Double) {
  def name: String
}

object PerformanceMetric {

  def all: Seq[PerformanceMetric] = {
    MeanIterationsMetric("Mean iterations for 90%", 0.9) ::
    MeanIterationsMetric("Mean iterations for 95%", 0.95) ::
    MeanIterationsMetric("Mean iterations for 100%", 0.999) :: Nil
  }

  private case class MeanIterationsMetric(name: String, targetFMeasure: Double) extends PerformanceMetric {
    def apply(result: RunResult) = {
      result.runs.map(_.iterations(targetFMeasure)).sum.toDouble / result.runs.size
    } 
  }
}