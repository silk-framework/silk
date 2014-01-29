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

package de.fuberlin.wiwiss.silk.plugins.distance.tokenbased

import de.fuberlin.wiwiss.silk.runtime.plugin.Plugin
import de.fuberlin.wiwiss.silk.linkagerule.similarity.DistanceMeasure
import de.fuberlin.wiwiss.silk.plugins.distance.characterbased.LevenshteinDistance

@Plugin(
  id = "softjaccard",
  categories = Array("Tokenbased"),
  label = "Soft Jaccard",
  description = "Soft Jaccard similarity coefficient. Same as Jaccard distance but values within an levenhstein distance of 'maxDistance' are considered equivalent."
)
case class SoftJaccardDistance(maxDistance: Int = 1) extends DistanceMeasure {

  private val levenshtein = LevenshteinDistance()

  private val jaccard = JaccardDistance()

  override def apply(values1: Traversable[String], values2: Traversable[String], limit: Double): Double = {
    //Replace all values in values1 with their equivalent in values2 while keeping values without any equivalent
    val values1Replaced = values1.flatMap{v1 =>
      val equivalentValues = values2.filter(v2 => levenshtein.evaluate(v1, v2, maxDistance) <= maxDistance)
      if(equivalentValues.isEmpty) Traversable(v1) else equivalentValues
    }

    jaccard(values1Replaced, values2)
  }

  override def index(values: Set[String], limit: Double) = {
    if(values.isEmpty) {
      //We index an empty value, so that the index is empty but has the right size
      levenshtein.indexValue("", limit)
    } else {
      //Determine the number of values we need to index
      val indexSize = math.round(values.size * limit + 0.5).toInt
      //Index each value separately and merge all indices
      values.take(indexSize).map(levenshtein.indexValue(_, limit)).reduce(_ merge _)
    }
  }
}