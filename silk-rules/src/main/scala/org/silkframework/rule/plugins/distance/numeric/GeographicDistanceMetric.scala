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

package org.silkframework.rule.plugins.distance.numeric

import org.silkframework.entity.Index
import org.silkframework.rule.similarity.SimpleDistanceMeasure
import org.silkframework.runtime.plugin.annotations.Plugin
import org.silkframework.util.StringUtils._

import scala.math._

/**
 * This metric takes geographical coordinates of two points,
 * given in degrees of longitude and latitude, and creates a score measuring
 * the nearness of those two points in air line distance. The default metric is the distance of the two points in meters but its behaviour is configurable
 * via the following parameters:
 * unit = "meter" or "m" , "kilometer" or "km" (default) - the unit in which the distance is measure
 * @author Konrad Höffner (AKSW, Uni Leipzig)
 */
@Plugin(
  id = "wgs84",
  categories = Array("Numeric"),
  label = "Geographical distance",
  description = "Computes the geographical distance between two points. Author: Konrad Höffner (MOLE subgroup of Research Group AKSW, University of Leipzig)")
case class GeographicDistanceMetric(unit: String = "km") extends SimpleDistanceMeasure {
  require(Set("m", "meter", "km", "kilometer").contains(unit), "Invalid unit: '" + unit + "'. Allowed units: \"m\", \"meter\", \"km\", \"kilometer\"")

  private val multipliers = Map("km" -> 0.001, "kilometer" -> 0.001, "meter" -> 1.0, "m" -> 1.0)

  private val unitMultiplier: Double = multipliers.get(unit).getOrElse(1)

  override def evaluate(str1: String, str2: String, limit: Double): Double = {
    //Parse the coordinates and return a similarity value if both coordinates could be extracted.
    (getCoordinates(str1), getCoordinates(str2)) match {
      case (Some(loc1), Some(loc2)) => getGeometricDistance(loc1, loc2)
      case _ => Double.PositiveInfinity
    }
  }

  override def emptyIndex(limit: Double): Index = {
    val emptyLatitude = Index.continuousEmpty(0.0, 1.0, normalizedLatitudeLimit(limit))
    val emptyLongitude = Index.continuousEmpty(0.0, 1.0,  normalizedLongitudeLimit(limit))

    emptyLatitude conjunction emptyLongitude
  }

  override def indexValue(str: String, limit: Double, sourceOrTarget: Boolean): Index = {
    getCoordinates(str) match {
      case Some(coords) => {
        val latIndex = indexLatitude((coords.lat + 90.0) / 180.0, limit)
        val lonIndex = indexLongitude((coords.long + 180.0) / 360.0 * cos(deg2rad(coords.lat)), limit)

        latIndex conjunction lonIndex
      }
      case None => emptyIndex(limit)
    }
  }

  private def indexLatitude(latitude: Double, limit: Double) = {
    Index.continuous(latitude, 0.0, 1.0, normalizedLatitudeLimit(limit))
  }

  private def indexLongitude(longitude: Double, limit: Double) = {
    Index.continuous(longitude, 0.0, 1.0, normalizedLongitudeLimit(limit))
  }

  private def normalizedLatitudeLimit(limit: Double): Double = {
    val earthCircumferenceEquatorial = 40075160.0
    limit / (earthCircumferenceEquatorial * unitMultiplier)
  }

  private def normalizedLongitudeLimit(limit: Double): Double = {
    val earthCircumferenceMeridional = 40008000.0
    limit / (earthCircumferenceMeridional * unitMultiplier)
  }

  /**
   * Extracts the latitude and longitude from a string and takes their order from the parameter "longitudeFirst".
   *
   * @param s The WGS84 coordinate string. Examples from DBpedia are:
   *          - POINT(-0.124722 51.5081) // first longitude then latitude, used by <http://www.w3.org/2003/01/geo/wgs84_pos#geometry>
   *          - 51.50805555555556 -0.12472222222222222 // first latitude then longitude, used by <http://www.georss.org/georss/point>
   * @return The coordinate if it could be extracted. None otherwise.
   */
  private def getCoordinates(s: String): Option[GeoPoint] = {
    val allowedCharacters = Set('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '.', '-', '+', ' ')
    val numbers = s.filter(allowedCharacters contains _)

    val geoPoint = numbers.split(" ") match {
      case Array(DoubleLiteral(c1), DoubleLiteral(c2)) => {
        if (s.startsWith("POINT"))
          Some(new GeoPoint(c2, c1))
        else
          Some(new GeoPoint(c1, c2))
      }
      case _ => None
    }

    geoPoint
  }

  /**
   * Computes the distance between two geo points.
   */
  private def getGeometricDistance(loc1: GeoPoint, loc2: GeoPoint): Double = {
    // formula from http://www.zipcodeworld.com/samples/distance.java.html
    val theta = loc1.long - loc2.long // 180
    var dist = sin(deg2rad(loc1.lat)) * sin(deg2rad(loc2.lat)) + cos(deg2rad(loc1.lat)) * cos(deg2rad(loc2.lat)) * cos(deg2rad(theta))
    if(dist >= 1.0) {
      0.0
    }
    else {
      dist = acos(dist)
      dist = rad2deg(dist)
      dist = dist * 60 * 1.1515 * 1.609344 * 1000; // in meters
      dist = dist * unitMultiplier
      dist
    }
  }

  /**
   * Converts decimal degrees to radians.
   */
  private def deg2rad(deg: Double): Double = {
    (deg * java.lang.Math.PI / 180.0)
  }

  /**
   * Converts radians to decimal degrees.
   */
  private def rad2deg(rad: Double): Double = {
    (rad * 180.0 / java.lang.Math.PI)
  }

  /**
   * Represents a geographical point.
   */
  private class GeoPoint(val lat: Double, val long: Double)

}
