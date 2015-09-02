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

import de.fuberlin.wiwiss.silk.rule.similarity.SimpleDistanceMeasure
import de.fuberlin.wiwiss.silk.util.StringUtils._
import de.fuberlin.wiwiss.silk.runtime.plugin.Plugin
import de.fuberlin.wiwiss.silk.entity.Index
import math.{min, max}
import de.fuberlin.wiwiss.silk.plugins.distance.tokenbased.JaccardDistance

/**
 * String similarity based on q-grams.
 *
 * Parameters:
 * - '''q''' (optional): The size of the sliding window. Default: 2
 */
@Plugin(
  id = "qGrams",
  categories = Array("Characterbased"),
  label = "qGrams",
  description = "String similarity based on q-grams (by default q=2)."
)
case class QGramsMetric(q: Int = 2, minChar: Char = '0', maxChar: Char = 'z') extends SimpleDistanceMeasure {
  private val jaccardCoefficient = JaccardDistance()

  //TODO test with toSet?
  override def evaluate(str1: String, str2: String, threshold: Double) = {
    jaccardCoefficient(str1.qGrams(q), str2.qGrams(q), threshold)
  }

  override def indexValue(value: String, limit: Double): Index = {
    val qGrams = value.qGrams(q)

    //The number of values we need to index
    val indexSize = math.round(qGrams.size * limit + 0.5).toInt

    val indices = qGrams.take(indexSize).map(indexQGram).toSet

    Index.oneDim(indices, BigInt(maxChar - minChar + 1).pow(q).toInt)
  }

  private def indexQGram(qGram: String): Int = {
    def combine(index: Int, char: Char) = {
      val croppedChar = min(max(char, minChar), maxChar)
      index * (maxChar - minChar + 1) + croppedChar - minChar
    }

    qGram.foldLeft(0)(combine)
  }
}
