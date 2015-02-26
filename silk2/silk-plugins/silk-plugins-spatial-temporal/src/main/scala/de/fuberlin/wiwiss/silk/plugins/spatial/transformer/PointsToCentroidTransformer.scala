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

package de.fuberlin.wiwiss.silk.plugins.spatial.transformer

import java.util.logging.Logger

import de.fuberlin.wiwiss.silk.linkagerule.input.Transformer
import de.fuberlin.wiwiss.silk.runtime.plugin.Plugin
import de.fuberlin.wiwiss.silk.plugins.spatial.utils._

/**
 * This plugin transforms a cluster of points expressed in W3C Geo vocabulary to their centroid expressed in WKT and WGS 84 (latitude-longitude).
 * In case that the literal is not a geometry, it is returned as it is.
 *
 * @author Panayiotis Smeros <psmeros@di.uoa.gr> (National and Kapodistrian University of Athens)
 */

@Plugin(
  id = "PointsToCentroidCTransformer",
  categories = Array("Spatial"),
  label = "Points-To-Centroid Transformer",
  description = "Transforms a cluster of points expressed in W3C Geo vocabulary to their centroid expressed in WKT and WGS 84 (latitude-longitude).")
case class PointsToCentroidTransformer() extends Transformer {

  override final def apply(values: Seq[Set[String]]): Set[String] = {

    val logger = Logger.getLogger(this.getClass.getName)

    values.length match {
      case 2 =>
        //2 inputs to the Transformer => assumes W3C Geo (lat and long) literals to be transformed.
        Utils.pointsToCentroidTransformer(values)
      case _ =>
        values.reduce(_ ++ _)
    }
  }
}