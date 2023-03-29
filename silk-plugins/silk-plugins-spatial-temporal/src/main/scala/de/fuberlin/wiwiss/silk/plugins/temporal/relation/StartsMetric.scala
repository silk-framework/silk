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

package org.silkframework.plugins.temporal.relation

import org.silkframework.entity.Index
import org.silkframework.plugins.temporal.utils._
import org.silkframework.rule.similarity.SingleValueDistanceMeasure
import org.silkframework.runtime.plugin.annotations.Plugin

/**
 * Computes the relation \"starts\" between two time periods or instants (It assumes that the times are expressed in the "yyyy-MM-DD'T'hh:mm:ss" format).
 * @author Panayiotis Smeros <psmeros@di.uoa.gr> (National and Kapodistrian University of Athens)
 */
@Plugin(
  id = "StartsMetric",
  categories = Array("Temporal"),
  label = "Starts",
  description = "Computes the relation \"starts\" between two time periods or instants.")
case class StartsMetric(blockingParameter: Double = 1.0) extends SingleValueDistanceMeasure {

  override def evaluate(str1: String, str2: String, limit: Double): Double = {
    Utils.evaluateRelation(str1, str2, Constants.STARTS)
  }

  override def indexValue(str: String, distance: Double, sourceOrTarget: Boolean): Index = {
    Utils.indexTimes(str, blockingParameter)
  }
}