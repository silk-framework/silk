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
