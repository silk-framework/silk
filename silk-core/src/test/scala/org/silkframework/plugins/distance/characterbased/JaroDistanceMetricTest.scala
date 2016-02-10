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

package org.silkframework.plugins.distance.characterbased

import org.scalatest.{FlatSpec, Matchers}
import org.silkframework.testutil.approximatelyEqualTo


class JaroDistanceMetricTest extends FlatSpec with Matchers {
  val metric = new JaroDistanceMetric()

  //Use cases from William E. Winkler : Overview of Record Linkage and Current Research Directions
  //Some tests are disabled because many web sources report different results
  "JaroDistanceMetric" should "pass the original test cases from William E. Winkler" in {
    sim("SHACKLEFORD", "SHACKELFORD") should be(approximatelyEqualTo(0.970))
    sim("DUNNINGHAM", "CUNNIGHAM") should be(approximatelyEqualTo(0.896))
    sim("NICHLESON", "NICHULSON") should be(approximatelyEqualTo(0.926))
    sim("JONES", "JOHNSON") should be(approximatelyEqualTo(0.790))
    sim("MASSEY", "MASSIE") should be(approximatelyEqualTo(0.889))
    sim("ABROMS", "ABRAMS") should be(approximatelyEqualTo(0.889))
    //sim("HARDIN", "MARTINEZ") should be (approximatelyEqualTo (0.722))
    //sim("ITMAN", "SMITH") should be (approximatelyEqualTo (0.000))
    sim("JERALDINE", "GERALDINE") should be(approximatelyEqualTo(0.926))
    sim("MARHTA", "MARTHA") should be(approximatelyEqualTo(0.944))
    sim("MICHELLE", "MICHAEL") should be(approximatelyEqualTo(0.869))
    sim("JULIES", "JULIUS") should be(approximatelyEqualTo(0.889))
    //sim("TANYA", "TONYA") should be (approximatelyEqualTo (0.867))
    sim("DWAYNE", "DUANE") should be(approximatelyEqualTo(0.822))
    sim("SEAN", "SUSAN") should be(approximatelyEqualTo(0.783))
    sim("JON", "JOHN") should be(approximatelyEqualTo(0.917))
    //sim("JON", "JAN") should be (approximatelyEqualTo (0.000))
  }

  "JaroDistanceMetric" should "be commutative" in {
    sim("DIXON", "DICKSONX") should be(approximatelyEqualTo(0.767))
    sim("DICKSONX", "DIXON") should be(approximatelyEqualTo(0.767))
    sim("MARTHA", "MARHTA") should be(approximatelyEqualTo(0.944))
    sim("MARHTA", "MARTHA") should be(approximatelyEqualTo(0.944))
  }

  private def sim(str1: String, str2: String) = 1.0 - metric.evaluate(str1, str2)
}