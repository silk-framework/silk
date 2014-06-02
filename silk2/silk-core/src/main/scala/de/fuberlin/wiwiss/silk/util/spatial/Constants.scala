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


/**
 * An object that contains all needed constants (namespaces, URIs, prefixes).
 * @author Panayiotis Smeros (Department of Informatics & Telecommunications, National & Kapodistrian University of Athens)
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
  val MAX_LAT = 90.0

  /**
   * Minimum Latitude (WGS 84 (latitude-longitude))
   */
  val MIN_LAT = -90.0

  /**
   * Maximum Longitude (WGS 84 (latitude-longitude))
   */
  val MAX_LONG = 180.0

  /**
   * Minimum Longitude (WGS 84 (latitude-longitude))
   */
  val MIN_LONG = -180.0

  /**
   * Latitude Range (WGS 84 (latitude-longitude))
   */
  val LAT_RANGE = (MAX_LAT - MIN_LAT).toInt
  
  /**
   * Longitude Range (WGS 84 (latitude-longitude))
   */
  val LONG_RANGE = (MAX_LONG - MIN_LONG).toInt
  
  /**
   * Spatial Distances
   */
  val CENTROID_DISTANCE = "centroidDistance"

  /**
   * Simple Features Topology Relations
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