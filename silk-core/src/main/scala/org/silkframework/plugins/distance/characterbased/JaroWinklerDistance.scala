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

import org.silkframework.rule.similarity.SimpleDistanceMeasure
import org.silkframework.runtime.plugin.Plugin

@Plugin(
  id = "jaroWinkler",
  categories = Array("Characterbased"),
  label = "Jaro-Winkler distance",
  description = "String similarity based on the Jaro-Winkler distance measure."
)
case class JaroWinklerDistance() extends SimpleDistanceMeasure {
  // maximum prefix length to use
  private final val MINPREFIXTESTLENGTH: Int = 4 //using value from lingpipe (was 6)

  // prefix adjustment scale
  private final val PREFIXADUSTMENTSCALE: Double = 0.1

  override def evaluate(str1: String, str2: String, threshold: Double) = {
    evaluateDistance(str1, str2)
  }

  /**
   * gets the similarity measure of the JaroWinkler metric for the given strings.
   *
   * @param string1
   * @param string2
   * @return 0 -1 similarity measure of the JaroWinkler metric
   */
  def evaluateDistance(string1: String, string2: String): Double = {
    val dist = JaroDinstanceMetric.jaro(string1, string2)
    val prefixLength = getPrefixLength(string1, string2)
    dist - (prefixLength.toDouble * PREFIXADUSTMENTSCALE * dist)
  }

  /**
   * gets the prefix length found of common characters at the begining of the strings.
   *
   * @param string1
   * @param string2
   * @return the prefix length found of common characters at the begining of the strings
   */
  private def getPrefixLength(string1: String, string2: String): Int = {
    val max: Int = math.min(MINPREFIXTESTLENGTH, math.min(string1.length, string2.length))

    var i: Int = 0
    while (i < max) {
      if (string1.charAt(i) != string2.charAt(i)) {
        return i
      }
      i += 1
    }
    max
  }
}