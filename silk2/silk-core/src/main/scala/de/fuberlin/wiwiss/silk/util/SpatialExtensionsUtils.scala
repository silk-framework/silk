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

package de.fuberlin.wiwiss.silk.util

import java.util.logging.Level
import java.util.logging.Logger

import com.vividsolutions.jts.geom.Geometry
import com.vividsolutions.jts.io.WKTReader
import com.vividsolutions.jts.operation.distance.DistanceOp.distance
import com.vividsolutions.jts.simplify.TopologyPreservingSimplifier

import de.fuberlin.wiwiss.silk.entity.Index

/**
 * Useful utils for the spatial extensions of Silk.
 * @author Panayiotis Smeros (Department of Informatics & Telecommunications, National & Kapodistrian University of Athens)
 */

object SpatialExtensionsUtils {

  private val logger = Logger.getLogger(this.getClass.getName)

  /**
   * This function simplifies a Geometry.
   *
   * @param geometryString : String
   * @param distanceTolerance: Double
   * @return String
   */
  def simplifyGeometry(geometryString: String, distanceTolerance: Double): String = {
    try {
      val geometry = Parser.WKTReader(geometryString, Constants.DEFAULT_SRID).get
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
      val geometry = Parser.WKTReader(geometryString, Constants.DEFAULT_SRID).get
      val centroid = geometry.getCentroid()

      //Create the index of the geometry based on its centroid.
      val latIndex = Index.continuous(centroid.getX(), Constants.MIN_LAT, Constants.MAX_LAT, distance)
      val longIndex = Index.continuous(centroid.getY(), Constants.MIN_LONG, Constants.MAX_LONG, distance)

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
      val geometry1 = Parser.WKTReader(geometryString1, Constants.DEFAULT_SRID).get
      val geometry2 = Parser.WKTReader(geometryString2, Constants.DEFAULT_SRID).get

      distanceType match {
        case Constants.CENTROID_DISTANCE => distance(geometry1.getCentroid(), geometry2.getCentroid())
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
      val geometry1 = Parser.WKTReader(geometryString1, Constants.DEFAULT_SRID).get
      val geometry2 = Parser.WKTReader(geometryString2, Constants.DEFAULT_SRID).get

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
      case Constants.EQUALS => geometry1.equals(geometry2)
      case Constants.DISJOINT => geometry1.disjoint(geometry2)
      case Constants.INTERSECTS => geometry1.intersects(geometry2)
      case Constants.TOUCHES => geometry1.touches(geometry2)
      case Constants.CROSSES => geometry1.crosses(geometry2)
      case Constants.WITHIN => geometry1.within(geometry2)
      case Constants.CONTAINS => geometry1.contains(geometry2)
      case Constants.OVERLAPS => geometry1.overlaps(geometry2)
      case _ => false
    }
  }

  /**
   * An stRDF/stSPARQL and GeoSPARQL parser.
   */
  object Parser {

    /**
     * This function reads a WKT Geometry.
     *
     * @param geometryString : String
     * @param SRID: Int
     * @return Option[Geometry]
     */
    def WKTReader(geometryString: String, srid: Int): Option[Geometry] = {

      val wktr = new WKTReader
      var geometry = wktr.read(geometryString)
      geometry.setSRID(srid)
      Option(geometry)
    }

    /**
     * This function reads a GML Geometry.
     *
     * @param geometryString : String
     * @return Option[Geometry]
     */
    def GMLReader(geometryString: String): Option[Geometry] = {

      //FIXME: This will be fixed, when the same bug will be fixed in Strabon.
      null.asInstanceOf[Option[Geometry]]

    }

    /**
     * This function parses a geometry literal (WKT or GML) according to the specifications of stRDF/stSPARQL and GeoSPARQL.
     *
     * ==WKT==
     * If the literal is an strdf:WKT or geo:wktLiteral literal concatenated with the SRID URI it returns the tuple (geometryString, SRID).
     * If the literal is a plain WKT literal it returns the tuple (geometryString, {@link Constants.DEFAULT_SRID}).
     *
     * ==GML==
     * If the literal is a GML literal it returns the tuple (geometryString, -1).
     * This happens because SRID is an attribute in the GML literal and it is parsed from the {@link GMLReader}.
     *
     * In any other case it returns (literal, {@link Constants.DEFAULT_SRID}).
     *
     * @param literal
     * @return (geometryString, SRID)
     */
    def separateGeometryFromSRID(literal: String): (String, Int) = {

      val trimmedLiteral = literal.trim
      var geometryString = null.asInstanceOf[String]
      var srid = null.asInstanceOf[Int]

      try {
        if (trimmedLiteral.length() != 0) {
          val index = trimmedLiteral.lastIndexOf(Constants.STRDF_SRID_DELIM)
          if (index > 0) {
            //strdf:WKT with SRID (assumes EPSG URI for SRID).
            geometryString = trimmedLiteral.substring(0, index)
            srid = augmentString(trimmedLiteral.substring((trimmedLiteral.lastIndexOf('/') + 1))).toInt
          } else {
            if (trimmedLiteral.startsWith("<http://")) {
              //Starts with a URI => geo:wktLiteral (assumes EPSG URI for SRID).
              val index = trimmedLiteral.indexOf('>')
              val URI = trimmedLiteral.substring(0, index)
              geometryString = trimmedLiteral.substring(index + 1).trim
              srid = augmentString(URI.substring(URI.lastIndexOf('/') + 1)).toInt
            } else if (trimmedLiteral.startsWith("<") && trimmedLiteral.contains("gml")) {
              //GML literal.
              geometryString = trimmedLiteral
              srid = -1
            } else {
              //Cannot guess the datatype, only plain literal was given.
              geometryString = trimmedLiteral
              srid = Constants.DEFAULT_SRID
            }
          }
        } else {
          //Empty geometry.
          geometryString = Constants.EMPTY_GEOM
          srid = Constants.DEFAULT_SRID
        }
      } catch {
        case e: Exception =>
          //In case of parse exception the literal is returned as it is.
          geometryString = trimmedLiteral
          srid = Constants.DEFAULT_SRID
      }
      logger.log(Level.ALL, "\nGeometry: " + geometryString + "\nSRID: " + srid)
      (geometryString, srid)
    }
  }

  /**
   * This function concatenates appropriately the values "lat" and "long" to create a single Point.
   *
   * @param lat  : Any
   * @param long : Any
   * @return POINT (lat, long) : String
   */
  def latLongConcat(lat: Any, long: Any): String = {

    "POINT (" + lat + " " + long + ")"
  }

  /**
   * An object that contains all needed constants (namespaces, URIs, prefixes).
   */
  object Constants {

    /**
     * The namespace for stRDF data model.
     */
    val stRDF = "http://strdf.di.uoa.gr/ontology#"

    /**
     * The namespace for GeoSPARQL ontology.
     */
    val GEO = "http://www.opengis.net/ont/geosparql#"

    /**
     * The namespace for GML.
     */
    val GML_OGC = "http://www.opengis.net/gml"

    /**
     * The URI for the datatype Well-Known Text (WKT).
     */
    val WKT = stRDF + "WKT"

    /**
     * The URI for the datatype Geography Markup Language (GML).
     */
    val GML = stRDF + "GML"

    /**
     * The URI for the datatype wktLiteral.
     */
    val WKTLITERAL = GEO + "wktLiteral"

    /**
     * The URI for the datatype gmlLiteral.
     */
    val GMLLITERAL = GEO + "gmlLiteral"

    /**
     * WKT representation for an empty geometry.
     */
    val EMPTY_GEOM = "MULTIPOLYGON EMPTY"

    /**
     * EPSG:4326 (default for stSPARQL).
     */
    val WGS84_LAT_LON_SRID = 4326

    /**
     * EPSG:3857 (default for GeoSPARQL).
     */
    val WGS84_LON_LAT_SRID = 3857

    /**
     * Default SRID.
     */
    val DEFAULT_SRID = WGS84_LAT_LON_SRID;

    /**
     * stRDF SRID delimiter.
     */
    val STRDF_SRID_DELIM = ";"

    /**
     * Maximum Latitude (WGS 84 (latitude-longitude))
     */
    val MAX_LAT = 180.0

    /**
     * Minimum Latitude (WGS 84 (latitude-longitude))
     */
    val MIN_LAT = -180.0

    /**
     * Maximum Longitude (WGS 84 (latitude-longitude))
     */
    val MAX_LONG = 90.0

    /**
     * Minimum Longitude (WGS 84 (latitude-longitude))
     */
    val MIN_LONG = -90.0

    /**
     * Spatial Distance
     */
    val CENTROID_DISTANCE = "centroid distance"

    /**
     * Topology Relations
     */
    val EQUALS = "equals"
    val DISJOINT = "disjoint"
    val INTERSECTS = "intersects"
    val TOUCHES = "touches"
    val CROSSES = "crosses"
    val WITHIN = "within"
    val CONTAINS = "contains"
    val OVERLAPS = "overlaps"
  }
}