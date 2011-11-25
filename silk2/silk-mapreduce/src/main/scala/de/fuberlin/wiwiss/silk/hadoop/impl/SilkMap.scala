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

package de.fuberlin.wiwiss.silk.hadoop.impl

import org.apache.hadoop.mapreduce.Mapper
import org.apache.hadoop.io.{NullWritable, Text}
import de.fuberlin.wiwiss.silk.hadoop.SilkConfiguration

class SilkMap extends Mapper[NullWritable, EntityPair, Text, EntityConfidence]
{
  protected override def map(key : NullWritable, entities : EntityPair, context : Mapper[NullWritable, EntityPair, Text, EntityConfidence]#Context)
  {
    val config = SilkConfiguration.get(context.getConfiguration)

    val confidence = config.linkSpec.rule(entities, 0.0)

    if(confidence >= 0.0)
    {
      context.write(new Text(entities.sourceEntity.uri), new EntityConfidence(confidence, entities.targetEntity.uri))
    }
  }
}
