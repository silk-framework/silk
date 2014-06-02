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

package de.fuberlin.wiwiss.silk.plugins.distance.temporal

import de.fuberlin.wiwiss.silk.entity.Index
import de.fuberlin.wiwiss.silk.linkagerule.similarity.SimpleDistanceMeasure
import de.fuberlin.wiwiss.silk.runtime.plugin.Plugin
import de.fuberlin.wiwiss.silk.util.temporal.TemporalExtensionsUtils.{evaluateDistance, indexTimes}
import de.fuberlin.wiwiss.silk.util.temporal.Constants._

/**
 * Computes the distance in hours between two time periods or instants (It assumes that the times are expressed in the "yyyy-MM-DD'T'hh:mm:ss" format).
 * @author Panayiotis Smeros (Department of Informatics & Telecommunications, National & Kapodistrian University of Athens)
 */
@Plugin(
  id = "HoursDistanceMetric",
  categories = Array("Temporal"),
  label = "Hours distance",
  description = "Computes the distance in hours between two time periods or instants. Author: Panayiotis Smeros (Department of Informatics & Telecommunications, National & Kapodistrian University of Athens)")
case class HoursDistanceMetric() extends SimpleDistanceMeasure {

  override def evaluate(str1: String, str2: String, limit: Double): Double = {
    evaluateDistance(str1, str2, limit, HOURS_DISTANCE)
  }

  override def indexValue(str: String, distance: Double): Index = {
    indexTimes(str, distance*MILLISECS_PER_HOUR)
  }
}