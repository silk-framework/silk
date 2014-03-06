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

package de.fuberlin.wiwiss.silk.plugins.distance.spatial

import math._
import de.fuberlin.wiwiss.silk.linkagerule.similarity.SimpleDistanceMeasure
import de.fuberlin.wiwiss.silk.util.StringUtils._
import de.fuberlin.wiwiss.silk.runtime.plugin.Plugin
import de.fuberlin.wiwiss.silk.entity.Index
import de.fuberlin.wiwiss.silk.util.SpatialExtensionsUtils
import com.vividsolutions.jts.operation.distance.DistanceOp.distance

/**
 * Computes the spatial containment between two geometries (It assumes that geometries are expressed in WKT and WGS 84 (latitude-longitude)).
 * @author Panayiotis Smeros (Department of Informatics & Telecommunications, National & Kapodistrian University of Athens)
 */
@Plugin(
  id = "SpatialContainmentMetric",
  categories = Array("Spatial"),
  label = "Spatial Containment",
  description = "Computes the spatial containment between two geometries. Author: Panayiotis Smeros (Department of Informatics & Telecommunications, National & Kapodistrian University of Athens)")
case class SpatialContainmentMetric() extends SimpleDistanceMeasure {

  override def evaluate(str1: String, str2: String, limit: Double): Double = {
    try {
      //Get the geometries.
      val geometry1 = SpatialExtensionsUtils.Parser.WKTReader(str1, SpatialExtensionsUtils.Constants.DEFAULT_SRID).get
      val geometry2 = SpatialExtensionsUtils.Parser.WKTReader(str2, SpatialExtensionsUtils.Constants.DEFAULT_SRID).get

      //Compute the spatial containment.
      if (geometry1.contains(geometry2))
        return 1.0
      else
        return Double.PositiveInfinity
        
    } catch {
      case e: Exception =>
        Double.PositiveInfinity
    }
  }

  override def indexValue(str: String, percentage: Double): Index = {
    try {
      val geometry = SpatialExtensionsUtils.Parser.WKTReader(str, SpatialExtensionsUtils.Constants.DEFAULT_SRID).get
      val centroid = geometry.getCentroid()

      //Create the index of the geometry based on its centroid.
      val latIndex = Index.continuous(centroid.getX(), SpatialExtensionsUtils.Constants.MIN_LAT, SpatialExtensionsUtils.Constants.MAX_LAT, percentage * (SpatialExtensionsUtils.Constants.MAX_LAT - SpatialExtensionsUtils.Constants.MIN_LAT))
      val longIndex = Index.continuous(centroid.getY(), SpatialExtensionsUtils.Constants.MIN_LONG, SpatialExtensionsUtils.Constants.MAX_LONG, percentage * (SpatialExtensionsUtils.Constants.MAX_LONG - SpatialExtensionsUtils.Constants.MIN_LONG))
      latIndex conjunction longIndex
    } catch {
      case e: Exception =>
        Index.empty
    }
  }
}