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

import de.fuberlin.wiwiss.silk.linkagerule.similarity.SimpleDistanceMeasure
import de.fuberlin.wiwiss.silk.util.plugin.Plugin

@Plugin(id = "jaro", label = "Jaro distance", description = "String similarity based on the Jaro distance metric.")
case class JaroDistanceMetric() extends SimpleDistanceMeasure {
  override def evaluate(str1: String, str2: String, threshold: Double) = {
    JaroDinstanceMetric.jaro(str1, str2)
  }
}

object JaroDinstanceMetric {
  def jaro(string1: String, string2: String): Double = {
    //get half the length of the string rounded up - (this is the distance used for acceptable transpositions)
    val halflen: Int = ((math.min(string1.length, string2.length)) / 2) + ((math.min(string1.length, string2.length)) % 2)

    //get common characters
    val common1 = getCommonCharacters(string1, string2, halflen)
    val common2 = getCommonCharacters(string2, string1, halflen)

    //check for zero in common
    if (common1.length == 0 || common2.length == 0) {
      return 1.0
    }
    /*
    //check for same length common strings returning 0.0 is not the same
    if (common1.length != common2.length) {
        return 0.0
    }
    */

    //get the number of transpositions
    var transpositions: Int = 0

    for (i <- 0 to math.min(common1.length - 1, common2.length - 1)) {
      if (common1.charAt(i) != common2.charAt(i)) {
        transpositions += 1
      }
    }

    transpositions = transpositions / 2

    //calculate jaro metric
    1.0 - ((common1.length / (string1.length.toDouble) + common2.length / (string2.length.toDouble) + (common1.length - transpositions) / (common1.length.toDouble)) / 3.0)
  }


  /**
   * returns a string buffer of characters from string1 within string2 if they are of a given
   * distance seperation from the position in string1.
   *
   * @param string1
   * @param string2
   * @param distanceSep
   * @return a string buffer of characters from string1 within string2 if they are of a given
   *         distance seperation from the position in string1
   */
  private def getCommonCharacters(string1: String, string2: String, distanceSep: Int): StringBuilder = {
    val returnCommons = new StringBuilder()
    val copy = new StringBuilder(string2)

    var i = 0
    for (string1Char <- string1) {
      var foundIt = false
      var j = math.max(0, i - distanceSep)
      while (!foundIt && j < math.min(i + distanceSep + 1, string2.length)) {
        if (copy.charAt(j) == string1Char) {
          foundIt = true
          returnCommons.append(string1Char)
          copy.setCharAt(j, 0.toChar)
        }
        j += 1
      }
      i += 1
    }

    returnCommons
  }
}
