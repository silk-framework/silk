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
import de.fuberlin.wiwiss.silk.linkagerule.input.SimpleTransformer
import scala.reflect.io._
import de.fuberlin.wiwiss.silk.util.SpatialExtensionsUtils
import com.vividsolutions.jts.geom.Geometry
import org.opengis.referencing.crs.CoordinateReferenceSystem
import org.geotools.referencing.CRS;
import org.geotools.geometry.jts.JTS;
import java.util.logging.{ Level, Logger }

/**
 * This transformer takes ???, given in ???, and transforms it ???.
 * @author Panayiotis Smeros (Department of Informatics & Telecommunications, National & Kapodistrian University of Athens)
 */

@Plugin(
  id = "CRSTransformer",
  categories = Array("Spatial"),
  label = "CRS Transformer",
  description = "Trasforms a geometry from any serialization (WKT or GML) and any Coordinate Reference System(CRS) to WKT and WGS 84 (latitude-longitude). Author: Panayiotis Smeros (Department of Informatics & Telecommunications, National & Kapodistrian University of Athens)")
case class CRSTransformer() extends SimpleTransformer {
  override def evaluate(literal: String): String = {

    val logger = Logger.getLogger(CRSTransformer.getClass.getName)

    var geometry = null.asInstanceOf[Option[Geometry]]
    var srid = null.asInstanceOf[Int]

    try {
      var (geometryString, srid) = SpatialExtensionsUtils.Parser.separateGeometryFromSRID(literal)

      srid match {
        case SpatialExtensionsUtils.Constants.DEFAULT_SRID =>
          return literal
        case -1 =>
          geometry = SpatialExtensionsUtils.Parser.GMLReader(geometryString)
        case _ =>
          geometry = SpatialExtensionsUtils.Parser.WKTReader(geometryString)
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

    try {
      val sourceCRS = CRS.decode("EPSG:" + srid)
      val targetCRS = CRS.decode("EPSG:" + SpatialExtensionsUtils.Constants.DEFAULT_SRID)
      val transform = CRS.findMathTransform(sourceCRS, targetCRS, true)

      return JTS.transform(geometry.get, transform).toText()
    } catch {
      case e: Exception =>
        logger.log(Level.ALL, "Tranformation Error. Returning literal as it is.")
        return literal
    }
  }
}