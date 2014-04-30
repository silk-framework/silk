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

package de.fuberlin.wiwiss.silk.plugins.transformer.spatial

import de.fuberlin.wiwiss.silk.runtime.plugin.Plugin
import de.fuberlin.wiwiss.silk.util.StringUtils._
import de.fuberlin.wiwiss.silk.linkagerule.input.Transformer
import de.fuberlin.wiwiss.silk.util.SpatialExtensionsUtils
import com.vividsolutions.jts.geom.Geometry
import org.opengis.referencing.crs.CoordinateReferenceSystem
import org.geotools.referencing.CRS;
import org.geotools.geometry.jts.JTS;
import java.util.logging.{ Level, Logger }

/**
 * This plugin transforms a cluster of points expressed in W3C Geo vocabulary to their centroid expressed in WKT and WGS 84 (latitude-longitude).
 * In case that the literal is not a geometry, it is returned as it is.
 *
 * @author Panayiotis Smeros (Department of Informatics & Telecommunications, National & Kapodistrian University of Athens)
 */

@Plugin(
  id = "PointsToCentroidCTransformer",
  categories = Array("Spatial"),
  label = "Points-To-Centroid Transformer",
  description = "Transforms a cluster of points expressed in W3C Geo vocabulary to their centroid expressed in WKT and WGS 84 (latitude-longitude). Author: Panayiotis Smeros (Department of Informatics & Telecommunications, National & Kapodistrian University of Athens)")
case class PointsToCentroidTransformer() extends Transformer {

  override final def apply(values: Seq[Set[String]]): Set[String] = {

    val logger = Logger.getLogger(this.getClass.getName)

    values.length match {
      case 2 =>
        //2 inputs to the Transformer => assumes W3C Geo (lat and long) literals to be transformed.
        var Seq(set1, set2) = values
        var lat = 0.0
        var long = 0.0

        //Computes the centroid.
        try {
          while (set1.iterator.hasNext)
            lat += set1.iterator.next.toFloat

          while (set2.iterator.hasNext)
            long += set2.iterator.next.toFloat
          lat /= set1.iterator.size
          long /= set2.iterator.size

          Set(SpatialExtensionsUtils.latLongConcat(lat, long))
        } catch {
          case e: Exception =>
            logger.log(Level.ALL, "Cast Error. Returning literal as it is.")
            values.reduce(_ ++ _)
        }
      case _ =>
        values.reduce(_ ++ _)
    }
  }
}