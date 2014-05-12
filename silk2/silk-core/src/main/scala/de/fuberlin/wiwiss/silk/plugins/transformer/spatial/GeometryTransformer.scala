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

import java.util.logging.Level
import java.util.logging.Logger

import org.geotools.geometry.jts.JTS
import org.geotools.referencing.CRS

import com.vividsolutions.jts.geom.Geometry

import de.fuberlin.wiwiss.silk.linkagerule.input.Transformer
import de.fuberlin.wiwiss.silk.runtime.plugin.Plugin
import de.fuberlin.wiwiss.silk.util.spatial.Constants._
import de.fuberlin.wiwiss.silk.util.spatial.Parser._

/**
 * This plugin transforms a geometry expressed in GeoSPARQL, stSPARQL or W3C Geo vocabulary from any serialization (WKT or GML) and any Coordinate Reference System (CRS) to WKT and WGS 84 (latitude-longitude).
 * In case that the literal is not a geometry, it is returned as it is.
 *
 * @author Panayiotis Smeros (Department of Informatics & Telecommunications, National & Kapodistrian University of Athens)
 */

@Plugin(
  id = "GeometryTransformer",
  categories = Array("Spatial"),
  label = "Geometry Transformer",
  description = "Trasforms a geometry expressed in GeoSPARQL, stSPARQL or W3C Geo vocabulary from any serialization (WKT or GML) and any Coordinate Reference System (CRS) to WKT and WGS 84 (latitude-longitude). Author: Panayiotis Smeros (Department of Informatics & Telecommunications, National & Kapodistrian University of Athens)")
case class GeometryTransformer() extends Transformer {

  override final def apply(values: Seq[Set[String]]): Set[String] = {

    values.length match {
      case 1 =>
        //1 input to the Transformer => assumes stSPARQL or GeoSPARQL literal to be transformed.
        values.iterator.next.map(stSPARQLGeoSPARQLTransformer)
      case 2 =>
        //2 inputs to the Transformer => assumes W3C Geo (lat and long) literals to be transformed.
        var Seq(set1, set2) = values

        if ((set1.iterator.size >= 1) && (set1.iterator.size >= 1)) {
          //Assumes that the first point corresponds to the geometry of the resource.
          Set(w3cGeoTransformer(set1.iterator.next, set2.iterator.next))
        } else
          values.reduce(_ ++ _)
      case _ =>
        values.reduce(_ ++ _)
    }
  }
  
  /**
   * This function transforms a geometry expressed in stSPARQL or GeoSPARQL from any serialization (WKT or GML) and any Coordinate Reference System (CRS) to WKT and WGS 84 (latitude-longitude).
   * In case that the literal is not a geometry, it is returned as it is.
   * 
   * @param literal : String
   * @return WKTLiteral || literal : String
   */
  def stSPARQLGeoSPARQLTransformer(literal: String): String = {

    val logger = Logger.getLogger(this.getClass.getName)

    var geometry = null.asInstanceOf[Option[Geometry]]
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

    if (!geometry.isDefined) {
      logger.log(Level.ALL, "Null Geometry. Returning literal as it is.")
      return literal
    }

    //Convert geometry to default SRID.
    try {
      val sourceCRS = CRS.decode("EPSG:" + geometry.get.getSRID())
      val targetCRS = CRS.decode("EPSG:" + DEFAULT_SRID)
      val transform = CRS.findMathTransform(sourceCRS, targetCRS, true)

      return JTS.transform(geometry.get, transform).toText()
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
}