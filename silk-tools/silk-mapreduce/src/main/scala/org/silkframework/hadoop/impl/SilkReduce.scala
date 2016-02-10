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

package org.silkframework.hadoop.impl

import org.apache.hadoop.io.Text
import org.apache.hadoop.mapreduce.Reducer

import scala.collection.JavaConversions._

class SilkReduce extends Reducer[Text, EntityConfidence, Text, EntityConfidence] {

  protected override def reduce(sourceUri : Text, entitiySimilarities : java.lang.Iterable[EntityConfidence],
                                context : Reducer[Text, EntityConfidence, Text, EntityConfidence]#Context) {
    val config = SilkConfiguration.get(context.getConfiguration)
    val threshold = config.linkSpec.rule.filter.threshold.getOrElse(-1.0)
    val resultsPerEntity = entitiySimilarities.toSeq.filter(_.similarity >= threshold).distinct
    config.linkSpec.rule.filter.limit match {
      case Some(limit) => {
        for(entitySimilarity <- resultsPerEntity.sortWith(_.similarity > _.similarity).take(limit)) {
          context.write(sourceUri, entitySimilarity)
        }
      }
      case None => {
        for(entitySimilarity <- resultsPerEntity) {
          context.write(sourceUri, entitySimilarity)
        }
      }
    }
  }
}
