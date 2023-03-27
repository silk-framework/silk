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
import org.silkframework.rule.similarity.{NormalizedDistanceMeasure, SingleValueDistanceMeasure}
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}

import scala.math.max

@Plugin(
  id = "levenshtein",
  categories = Array("Characterbased"),
  label = "Normalized Levenshtein distance",
  description = "Normalized Levenshtein distance. Divides the edit distance by the length of the longer string."
)
@DistanceMeasureExamples(Array(
  new DistanceMeasureExample(
    description = "Returns 0 for equal strings.",
    input1 = Array("John"),
    input2 = Array("John"),
    output = 0.0
  ),
  new DistanceMeasureExample(
    description = "Returns 1/4 if two strings of length 4 differ by one edit operation.",
    input1 = Array("John"),
    input2 = Array("Jxhn"),
    output = 0.25
  ),
  new DistanceMeasureExample(
    description = "Normalizes the edit distance by the length of the longer string.",
    input1 = Array("John"),
    input2 = Array("Jhn"),
    output = 0.25
  ),
  new DistanceMeasureExample(
    description = "Returns the maximum distance of 1 for completely different strings.",
    input1 = Array("John"),
    input2 = Array("Clara"),
    output = 1.0
  )
))
case class LevenshteinMetric(
  @Param(label = "Q-grams size", value = "The size of the q-grams to be indexed. Setting this to zero will disable indexing.", advanced = true)
  qGramsSize: Int = 2,
  @Param(value = "The minimum character that is used for indexing", advanced = true)
  minChar: Char = '0',
  @Param(value = "The maximum character that is used for indexing", advanced = true)
  maxChar: Char = 'z') extends SingleValueDistanceMeasure with NormalizedDistanceMeasure {

  private val levenshtein = LevenshteinDistance(qGramsSize, minChar, maxChar)

  override def evaluate(str1: String, str2: String, limit: Double): Double = {
    val scale = max(str1.length, str2.length)
    levenshtein.evaluate(str1, str2, limit * scale) / scale
  }

  override def emptyIndex(limit: Double): Index = {
    levenshtein.emptyIndex(limit)
  }

  override def indexValue(str: String, limit: Double, sourceOrTarget: Boolean): Index = {
    levenshtein.indexValue(str, limit * str.length, sourceOrTarget)
  }
}
