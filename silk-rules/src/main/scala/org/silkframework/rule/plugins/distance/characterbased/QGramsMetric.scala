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

package org.silkframework.rule.plugins.distance.characterbased

import org.silkframework.entity.Index
import org.silkframework.rule.annotations.{DistanceMeasureExample, DistanceMeasureExamples}
import org.silkframework.rule.plugins.distance.tokenbased.JaccardDistance
import org.silkframework.rule.similarity.{NormalizedDistanceMeasure, SingleValueDistanceMeasure}
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}
import org.silkframework.util.StringUtils._

import scala.math.{max, min}

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
@DistanceMeasureExamples(Array(
  new DistanceMeasureExample(
    description = "Returns 0.0 if the input strings are equal.",
    input1 = Array("abcd"),
    input2 = Array("abcd"),
    output = 0.0
  ),
  new DistanceMeasureExample(
    description = "Returns 1.0 if the input strings do not share a single q-gram.",
    input1 = Array("abcd"),
    input2 = Array("dcba"),
    output = 1.0
  ),
  new DistanceMeasureExample(
    description = "Returns 1 minus the matching q-grams divided by the total number of q-grams. Generated q-grams in this example: (#a, ab, b#) and (#a, ac, c#).",
    input1 = Array("ab"),
    input2 = Array("ac"),
    output = 0.8
  )
))
case class QGramsMetric(q: Int = 2,
                        @Param(value = "The minimum character that is used for indexing", advanced = true)
                        minChar: Char = '0',
                        @Param(value = "The maximum character that is used for indexing", advanced = true)
                        maxChar: Char = 'z') extends SingleValueDistanceMeasure with NormalizedDistanceMeasure {

  private val jaccardCoefficient = JaccardDistance()

  //TODO test with toSet?
  override def evaluate(str1: String, str2: String, threshold: Double) = {
    jaccardCoefficient(str1.qGrams(q), str2.qGrams(q), threshold)
  }

  override def emptyIndex(limit: Double): Index = {
    Index.oneDim(Set.empty, BigInt(maxChar - minChar + 1).pow(q).toInt)
  }

  override def indexValue(value: String, limit: Double, sourceOrTarget: Boolean): Index = {
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
