package de.fuberlin.wiwiss.silk.hadoop.impl

import org.apache.hadoop.mapreduce.Mapper
import org.apache.hadoop.io.{NullWritable, Text}
import de.fuberlin.wiwiss.silk.hadoop.SilkConfiguration

class SilkMap extends Mapper[NullWritable, InstancePair, Text, InstanceConfidence]
{
  protected override def map(key : NullWritable, instances : InstancePair, context : Mapper[NullWritable, InstancePair, Text, InstanceConfidence]#Context)
  {
    val config = SilkConfiguration.get(context.getConfiguration)

    val confidence = config.linkSpec.rule(instances, 0.0)

    if(confidence >= 0.0)
    {
      context.write(new Text(instances.sourceInstance.uri), new InstanceConfidence(confidence, instances.targetInstance.uri))
    }
  }
}
