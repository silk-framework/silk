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

package org.silkframework.rule.similarity

import org.silkframework.entity.Index
import org.silkframework.runtime.plugin.annotations.PluginType
import org.silkframework.runtime.plugin.{AnyPlugin, PluginFactory}

@PluginType(
  customDescription = classOf[DistanceMeasureDescriptionGenerator]
)
trait DistanceMeasure extends AnyPlugin {

  /**
   * Computes the distance between two sets of strings.
   *
   * @param values1 The first sequence of strings.
   * @param values2 The second sequence of strings.
   * @param limit If the expected distance between both sets exceeds this limit, this method may
   *              return Double.PositiveInfinity instead of the actual distance in order to save computation time.
   * @return A positive number that denotes the computed distance between both strings.
   */
  def apply(values1: Seq[String], values2: Seq[String], limit: Double = Double.PositiveInfinity): Double

  /**
    * Indexes a sequence of values.
    *
    * @param values The values.
    * @param limit Two values that are closer than this limit receive have a overlapping index.
    * @param sourceOrTarget True, if the source values should be indexed.
    *                       False, if the target values should be indexed.
    * @return
    */
  def index(values: Seq[String], limit: Double, sourceOrTarget: Boolean): Index = Index.default

  /**
    * Checks if the provided threshold is valid for this measure.
    *
    * @return None, if the threshold is valid
    *         An error message, otherwise.
    */
  def validateThreshold(threshold: Double): Option[String] = {
    if(threshold < 0.0) {
      Some(s"Threshold must be greater than 0.")
    } else {
      None
    }
  }

  /**
   * True, if this distance measure is normalized, i.e., it returns a value between 0 and 1.
   */
  def isNormalized: Boolean = false
}

object DistanceMeasure extends PluginFactory[DistanceMeasure]
