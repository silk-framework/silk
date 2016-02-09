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

package org.silkframework.plugins.spatial.transformer

import org.scalatest.{FlatSpec, Matchers}



/**
 * Tests the Geometry Transformer.
 * @author Panayiotis Smeros <psmeros@di.uoa.gr> (National and Kapodistrian University of Athens)
 */


class GeometryTransformerTest extends FlatSpec with Matchers {

  val transformer = new GeometryTransformer()

  //Without SRID.
  "GeometryTransformer test 1" should "return 'Set(\"Point(1 0)\")'" in {
    transformer.apply(Seq(Seq("Point(1 0)"))) should equal(Seq("Point(1 0)"))
  }

  //With default SRID (GeoSPARQL).
  "GeometryTransformer test 2" should "return 'Set(\"POINT (1 0)\")'" in {
    transformer.apply(Seq(Seq("<http://www.opengis.net/def/crs/EPSG/0/4326> POINT (1 0)"))) should equal(Seq("POINT (1 0)"))
  }

  //With non-default SRID (GeoSPARQL).
  "GeometryTransformer test 3" should "return 'Set(\"POINT (0 0)\")'" in {
    transformer.apply(Seq(Seq("<http://www.opengis.net/def/crs/EPSG/0/3857> POINT (0 0)"))) should equal(Seq("POINT (0 0)"))
  }

  //With default SRID (stSPARQL).
  "GeometryTransformer test 4" should "return 'Set(\"POINT (1 0)\")'" in {
    transformer.apply(Seq(Seq("POINT (1 0);http://www.opengis.net/def/crs/EPSG/0/4326"))) should equal(Seq("POINT (1 0)"))
  }

  //With non-default SRID (stSPARQL).
  "GeometryTransformer test 5" should "return 'Set(\"POINT (0 0)\")'" in {
    transformer.apply(Seq(Seq("POINT (0 0);http://www.opengis.net/def/crs/EPSG/0/3857"))) should equal(Seq("POINT (0 0)"))
  }

  //With 2 arguments (W3C Geo).
  "GeometryTransformer test 6" should "return 'Set(\"POINT (1 0)\")'" in {
    transformer.apply(Seq(Seq("1"), Seq("0"))) should equal(Seq("POINT (1 0)"))
  }  
}
