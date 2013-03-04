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

package de.fuberlin.wiwiss.silk.plugins.distance.characterbased

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import de.fuberlin.wiwiss.silk.plugins.util.approximatelyEqualTo

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class JaroWinklerMetricTest extends FlatSpec with ShouldMatchers {
  val metric = new JaroWinklerDistance()

  //Use cases from William E. Winkler : Overview of Record Linkage and Current Research Directions
  //Some tests are disabled because many web sources report different results
  "JaroWinklerDistance" should "pass the original test cases from William E. Winkler" in {
    //sim("SHACKLEFORD", "SHACKELFORD") should be (approximatelyEqualTo (0.982))
    sim("DUNNINGHAM", "CUNNIGHAM") should be(approximatelyEqualTo(0.896))
    sim("NICHLESON", "NICHULSON") should be(approximatelyEqualTo(0.956))
    sim("JONES", "JOHNSON") should be(approximatelyEqualTo(0.832))
    sim("MASSEY", "MASSIE") should be(approximatelyEqualTo(0.933))
    sim("ABROMS", "ABRAMS") should be(approximatelyEqualTo(0.922))
    //sim("HARDIN", "MARTINEZ") should be (approximatelyEqualTo (0.000))
    //sim("ITMAN", "SMITH") should be (approximatelyEqualTo (0.000))
    sim("JERALDINE", "GERALDINE") should be(approximatelyEqualTo(0.926))
    sim("MARHTA", "MARTHA") should be(approximatelyEqualTo(0.961))
    sim("MICHELLE", "MICHAEL") should be(approximatelyEqualTo(0.921))
    sim("JULIES", "JULIUS") should be(approximatelyEqualTo(0.933))
    //sim("TANYA", "TONYA") should be (approximatelyEqualTo (0.880))
    sim("DWAYNE", "DUANE") should be(approximatelyEqualTo(0.840))
    sim("SEAN", "SUSAN") should be(approximatelyEqualTo(0.805))
    sim("JON", "JOHN") should be(approximatelyEqualTo(0.933))
    //sim("JON", "JAN") should be (approximatelyEqualTo (0.000))
  }

  "JaroWinklerDistance" should "be commutative" in {
    sim("JONES", "JOHNSON") should be(approximatelyEqualTo(0.832))
    sim("JOHNSON", "JONES") should be(approximatelyEqualTo(0.832))
  }

  private def sim(str1: String, str2: String) = 1.0 - metric.evaluate(str1, str2)
}