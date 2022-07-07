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
import org.silkframework.rule.similarity.SimpleDistanceMeasure
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}

import scala.math.max

@Plugin(
  id = "levenshtein",
  categories = Array("Characterbased"),
  label = "Normalized Levenshtein distance",
  description = "Normalized Levenshtein distance.")
case class LevenshteinMetric(
  @Param(value = "The minimum character that is used for indexing", advanced = true)
  minChar: Char = '0',
  @Param(value = "The maximum character that is used for indexing", advanced = true)
  maxChar: Char = 'z') extends SimpleDistanceMeasure {

  private val levenshtein = new LevenshteinDistance(minChar, maxChar)

  override def evaluate(str1: String, str2: String, limit: Double) = {
    val scale = max(str1.length, str2.length)

    levenshtein.evaluate(str1, str2, limit * scale) / scale
  }

  override def emptyIndex(limit: Double): Index = levenshtein.emptyIndex(limit)

  override def indexValue(str: String, limit: Double, sourceOrTarget: Boolean): Index = {
    levenshtein.indexValue(str, limit * str.length, sourceOrTarget)
  }
}
