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

package de.fuberlin.wiwiss.silk.plugins.spatial.relation

import de.fuberlin.wiwiss.silk.rule.similarity.SimpleDistanceMeasure
import de.fuberlin.wiwiss.silk.runtime.plugin.Plugin
import de.fuberlin.wiwiss.silk.entity.Index
import de.fuberlin.wiwiss.silk.plugins.spatial.utils._



/**
 * Computes the relation \"touches\" between two geometries (It assumes that geometries are expressed in WKT and WGS 84 (latitude-longitude)).
 * @author Panayiotis Smeros <psmeros@di.uoa.gr> (National and Kapodistrian University of Athens)
 */
@Plugin(
  id = "TouchesMetric",
  categories = Array("Spatial"),
  label = "Touches",
  description = "Computes the relation \"touches\" between two geometries.")
case class TouchesMetric(blockingParameter: Double = 1.0) extends SimpleDistanceMeasure {

  override def evaluate(str1: String, str2: String, limit: Double): Double = {
    Utils.evaluateRelation(str1, str2, limit, Constants.TOUCHES)
  }

  override def indexValue(str: String, distance: Double): Index = {
    Utils.indexGeometriesByEnvelope(str, blockingParameter)
  }
}