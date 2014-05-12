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

package de.fuberlin.wiwiss.silk.util.spatial

import java.util.logging.Level
import java.util.logging.Logger

import com.vividsolutions.jts.geom.Geometry
import com.vividsolutions.jts.io.WKTReader
import com.vividsolutions.jts.operation.distance.DistanceOp.distance
import com.vividsolutions.jts.simplify.TopologyPreservingSimplifier

import de.fuberlin.wiwiss.silk.entity.Index
import de.fuberlin.wiwiss.silk.util.spatial.Constants._

/**
 * Useful utils for the spatial extensions of Silk.
 * @author Panayiotis Smeros (Department of Informatics & Telecommunications, National & Kapodistrian University of Athens)
 */

object SpatialExtensionsUtils {

  private val logger = Logger.getLogger(this.getClass.getName)

  /**
   * This function returns the Envelope of a Geometry.
   *
   * @param geometryString : String
   * @return String
   */
  def getEnvelopeFromGeometry(geometryString: String): String = {
    try {
      Parser.WKTReader(geometryString, DEFAULT_SRID).get.getEnvelope().toText()
    } catch {
      case e: Exception =>
        geometryString
    }
  }

  /**
   * This function simplifies a Geometry.
   *
   * @param geometryString : String
   * @param distanceTolerance: Double
   * @return String
   */
  def simplifyGeometry(geometryString: String, distanceTolerance: Double): String = {
    try {
      val geometry = Parser.WKTReader(geometryString, DEFAULT_SRID).get
      TopologyPreservingSimplifier.simplify(geometry, distanceTolerance).toText()
    } catch {
      case e: Exception =>
        geometryString
    }
  }

  /**
   * This function indexes Geometries.
   *
   * @param geometryString : String
   * @param distance: Double
   * @return Index
   */
  def indexGeometries(geometryString: String, distance: Double): Index = {
    try {
      val geometry = Parser.WKTReader(geometryString, DEFAULT_SRID).get
      val centroid = geometry.getCentroid()

      //Create the index of the geometry based on its centroid.
      val latIndex = Index.continuous(centroid.getX(), MIN_LAT, MAX_LAT, distance)
      val longIndex = Index.continuous(centroid.getY(), MIN_LONG, MAX_LONG, distance)

      latIndex conjunction longIndex
    } catch {
      case e: Exception =>
        Index.empty
    }
  }

  /**
   * This function evaluates a distance between two Geometries.
   *
   * @param geometryString1 : String
   * @param geometryString2 : String
   * @param limit: Double
   * @param distanceType: String
   * @return Double
   */
  def evaluateDistance(geometryString1: String, geometryString2: String, limit: Double, distanceType: String): Double = {
    try {
      //Get the geometries.
      val geometry1 = Parser.WKTReader(geometryString1, DEFAULT_SRID).get
      val geometry2 = Parser.WKTReader(geometryString2, DEFAULT_SRID).get

      distanceType match {
        case CENTROID_DISTANCE => distance(geometry1.getCentroid(), geometry2.getCentroid())
      }

    } catch {
      case e: Exception =>
        Double.PositiveInfinity
    }
  }

  /**
   * This function evaluates a relation between two Geometries.
   *
   * @param geometryString1 : String
   * @param geometryString2 : String
   * @param limit: Double
   * @param relation: String
   * @return Double
   */
  def evaluateRelation(geometryString1: String, geometryString2: String, limit: Double, relation: String): Double = {
    try {
      //Get the geometries.
      val geometry1 = Parser.WKTReader(geometryString1, DEFAULT_SRID).get
      val geometry2 = Parser.WKTReader(geometryString2, DEFAULT_SRID).get

      //Compute the spatial relation.
      if (relate(geometry1, geometry2, relation))
        return limit
      else
        return Double.PositiveInfinity

    } catch {
      case e: Exception =>
        Double.PositiveInfinity
    }
  }

  /**
   * This function returns true if the given relation holds between two geometries.
   *
   * @param geometry1 : Geometry
   * @param geometry2 : Geometry
   * @param relation: String
   * @return Boolean
   */
  def relate(geometry1: Geometry, geometry2: Geometry, relation: String): Boolean = {
    relation match {
      case EQUALS => geometry1.equals(geometry2)
      case DISJOINT => geometry1.disjoint(geometry2)
      case INTERSECTS => geometry1.intersects(geometry2)
      case TOUCHES => geometry1.touches(geometry2)
      case CROSSES => geometry1.crosses(geometry2)
      case WITHIN => geometry1.within(geometry2)
      case CONTAINS => geometry1.contains(geometry2)
      case OVERLAPS => geometry1.overlaps(geometry2)
      case _ => false
    }
  }

}