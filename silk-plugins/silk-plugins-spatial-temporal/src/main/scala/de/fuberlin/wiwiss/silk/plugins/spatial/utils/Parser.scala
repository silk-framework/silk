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

package org.silkframework.plugins.spatial.utils

import java.util.logging.Level
import java.util.logging.Logger

import com.vividsolutions.jts.geom.Geometry
import com.vividsolutions.jts.io.WKTReader

/**
 * An stRDF/stSPARQL and GeoSPARQL parser.
 * @author Panayiotis Smeros <psmeros@di.uoa.gr> (National and Kapodistrian University of Athens)
 */

object Parser {

  private val logger = Logger.getLogger(this.getClass.getName)

  /**
   * This function reads a WKT Geometry.
   *
   * @param geometryString : String
   * @param SRID: Int
   * @return Geometry
   */
  def WKTReader(geometryString: String, srid: Int): Geometry = {

    val wktr = new WKTReader
    var geometry = wktr.read(geometryString)
    geometry.setSRID(srid)
    geometry
  }

  /**
   * This function reads a GML Geometry.
   *
   * @param geometryString : String
   * @return Geometry
   */
  def GMLReader(geometryString: String): Geometry = {

    //FIXME: This will be fixed, when the same bug will be fixed in Strabon.
    null

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
  
}