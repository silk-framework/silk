/* 
 * Copyright 2009-2011 Freie Universität Berlin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.fuberlin.wiwiss.silk.plugins.metric

import de.fuberlin.wiwiss.silk.linkagerule.similarity.DistanceMeasure
import de.fuberlin.wiwiss.silk.util.plugin.Plugin
import de.fuberlin.wiwiss.silk.entity.Index

@Plugin(id = "jaccard", label = "Jaccard", description = "Jaccard similarity coefficient.")
case class JaccardDistance() extends DistanceMeasure {

  override def apply(values1: Traversable[String], values2: Traversable[String], limit: Double): Double = {
    val set1 = values1.toSet
    val set2 = values2.toSet

    val intersectionSize = (set1 intersect set2).size
    val unionSize = (set1 union set2).size

    1.0 - intersectionSize.toDouble / unionSize
  }

  override def index(values: Set[String], limit: Double) = {
    //The number of values we need to index
    val indexSize = math.round(values.size * limit + 0.5).toInt

    Index.oneDim(values.take(indexSize).map(_.hashCode))
  }
}