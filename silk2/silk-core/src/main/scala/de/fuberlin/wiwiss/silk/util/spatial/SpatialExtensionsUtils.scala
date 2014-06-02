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
import com.vividsolutions.jts.operation.distance.DistanceOp.distance
import com.vividsolutions.jts.simplify.TopologyPreservingSimplifier
import com.vividsolutions.jts.simplify.DouglasPeuckerSimplifier
import com.vividsolutions.jts.algorithm.MinimumBoundingCircle

import org.geotools.geometry.jts.JTS
import org.geotools.referencing.CRS

import de.fuberlin.wiwiss.silk.entity.Index
import de.fuberlin.wiwiss.silk.util.spatial.Constants._
import de.fuberlin.wiwiss.silk.util.spatial.Parser._

/**
 * Useful utils for the spatial extensions of Silk.
 * @author Panayiotis Smeros (Department of Informatics & Telecommunications, National & Kapodistrian University of Athens)
 */

object SpatialExtensionsUtils {

  private val logger = Logger.getLogger(this.getClass.getName)

  /**
   * This function transforms a geometry expressed in stSPARQL or GeoSPARQL from any serialization (WKT or GML) and any Coordinate Reference System (CRS) to WKT and WGS 84 (latitude-longitude).
   * In case that the literal is not a geometry, it is returned as it is.
   *
   * @param literal : String
   * @return WKTLiteral || literal : String
   */
  def stSPARQLGeoSPARQLTransformer(literal: String): String = {

    val logger = Logger.getLogger(this.getClass.getName)

    var geometry = null.asInstanceOf[Geometry]
    var (geometryString, srid) = separateGeometryFromSRID(literal)

    try {
      srid match {
        case DEFAULT_SRID =>
          //Default SRID => no need for transformation.
          return geometryString
        case -1 =>
          geometry = GMLReader(geometryString)
        case _ =>
          geometry = WKTReader(geometryString, srid)
      }
    } catch {
      case e: Exception =>
        logger.log(Level.ALL, "Parse Error. Returning literal as it is.")
        return literal
    }

    if (geometry == null) {
      logger.log(Level.ALL, "Null Geometry. Returning literal as it is.")
      return literal
    }

    //Convert geometry to default SRID.
    try {
      val sourceCRS = CRS.decode("EPSG:" + geometry.getSRID())
      val targetCRS = CRS.decode("EPSG:" + DEFAULT_SRID)
      val transform = CRS.findMathTransform(sourceCRS, targetCRS, true)

      return JTS.transform(geometry, transform).toText()
    } catch {
      case e: Exception =>
        logger.log(Level.ALL, "Tranformation Error. Returning literal as it is.")
        return literal
    }
  }

  /**
   * This function transforms a geometry expressed in the W3C Geo Vocabulary to WKT.
   * It concatenates appropriately the values "lat" and "long" to create a single Point.
   * This point is already in WGS 84 (latitude-longitude) CRS, so there is no need for transformation.
   *
   * @param lat  : Any
   * @param long : Any
   * @return POINT (lat, long) : String
   */
  def w3cGeoTransformer(lat: Any, long: Any): String = {

    latLongConcat(lat, long)
  }

  /**
   * This function transforms a cluster of points to their centroid.
   *
   * @param points: Seq[Set[String]]
   * @return Set[String]
   */
  def pointsToCentroidTransformer(points: Seq[Set[String]]): Set[String] = {

    val Seq(set1, set2) = points
    val iterator1 = set1.iterator
    val iterator2 = set2.iterator
    var lat = 0.0
    var long = 0.0

    //Computes the centroid.
    try {
      while (iterator1.hasNext)
        lat += iterator1.next.toFloat
      while (iterator2.hasNext)
        long += iterator2.next.toFloat

      lat /= set1.iterator.size
      long /= set2.iterator.size

      Set(latLongConcat(lat, long))
    } catch {
      case e: Exception =>
        logger.log(Level.ALL, "Cast Error. Returning literal as it is.")
        points.reduce(_ ++ _)
    }
  }

  /**
   * This function returns the Envelope of a Geometry.
   *
   * @param geometryString : String
   * @return String
   */
  def getEnvelopeFromGeometry(geometryString: String): String = {
    try {
      WKTReader(geometryString, DEFAULT_SRID).getEnvelope().toText()
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
  def simplifyGeometry(geometryString: String, distanceTolerance: Double, preserveTopology: Boolean): String = {
    try {
      val geometry = WKTReader(geometryString, DEFAULT_SRID)
      if (preserveTopology)
        TopologyPreservingSimplifier.simplify(geometry, distanceTolerance).toText()
      else
        DouglasPeuckerSimplifier.simplify(geometry, distanceTolerance).toText()
    } catch {
      case e: Exception =>
        geometryString
    }
  }

  /**
   * This function indexes Geometries by their Centre.
   *
   * @param geometryString : String
   * @param distance: Double
   * @return Index
   */
  def indexGeometriesByCentre(geometryString: String, distance: Double): Index = {
    try {
      val geometry = WKTReader(geometryString, DEFAULT_SRID)
      val centre = new MinimumBoundingCircle(geometry).getCentre()

      val latIndex = Index.continuous(centre.y, MIN_LAT, MAX_LAT, distance)
      val longIndex = Index.continuous(centre.x, MIN_LONG, MAX_LONG, distance)

      latIndex conjunction longIndex
    } catch {
      case e: Exception =>
        Index.empty
    }
  }

  /**
   * This function indexes Geometries by their Envelope.
   *
   * @param geometryString : String
   * @param distance: Double
   * @return Index
   */
  def indexGeometriesByEnvelope(geometryString: String, distance: Double): Index = {
    try {
      val geometry = WKTReader(geometryString, DEFAULT_SRID)
      val envelope = geometry.getEnvelopeInternal()

      val blockCountLat = LAT_RANGE
      val blockCountLong = LONG_RANGE

      val minLatBlock = envelope.getMinY().toInt
      val maxLatBlock = envelope.getMaxY().toInt
      val minLongBlock = envelope.getMinX().toInt
      val maxLongBlock = envelope.getMaxX().toInt

      val latBlocks = for (i <- minLatBlock to maxLatBlock) yield i
      val longBlocks = for (i <- minLongBlock to maxLongBlock) yield i

      Index.oneDim(latBlocks.toSet, blockCountLat) conjunction Index.oneDim(longBlocks.toSet, blockCountLong)

    } catch {
      case e: Exception =>
        Index.empty
    }
  }

  /**
   * Choose function for indexing.
   */
  def indexGeometries = indexGeometriesByEnvelope(_, _)

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
      val geometry1 = WKTReader(geometryString1, DEFAULT_SRID)
      val geometry2 = WKTReader(geometryString2, DEFAULT_SRID)

      distanceType match {
        case CENTROID_DISTANCE => JTS.orthodromicDistance(geometry1.getCentroid().getCoordinate(), geometry2.getCentroid().getCoordinate(), CRS.decode("EPSG:" + DEFAULT_SRID))
        case _ => Double.PositiveInfinity
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
      val geometry1 = WKTReader(geometryString1, DEFAULT_SRID)
      val geometry2 = WKTReader(geometryString2, DEFAULT_SRID)

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