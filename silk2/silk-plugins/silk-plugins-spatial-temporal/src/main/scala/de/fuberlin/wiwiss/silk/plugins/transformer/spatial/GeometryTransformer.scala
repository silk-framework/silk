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

import de.fuberlin.wiwiss.silk.linkagerule.input.Transformer
import de.fuberlin.wiwiss.silk.runtime.plugin.Plugin
import de.fuberlin.wiwiss.silk.util.spatial.SpatialExtensionsUtils.{stSPARQLGeoSPARQLTransformer, w3cGeoTransformer}

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
}