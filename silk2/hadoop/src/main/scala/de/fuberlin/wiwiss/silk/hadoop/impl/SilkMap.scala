package de.fuberlin.wiwiss.silk.hadoop.impl

import org.apache.hadoop.mapreduce.Mapper
import org.apache.hadoop.io.{NullWritable, Text}
import de.fuberlin.wiwiss.silk.hadoop.SilkConfiguration

class SilkMap extends Mapper[NullWritable, InstancePair, Text, InstanceSimilarity]
{
    protected override def map(key : NullWritable, instances : InstancePair, context : Mapper[NullWritable, InstancePair, Text, InstanceSimilarity]#Context)
    {
        val config = SilkConfiguration.get(context.getConfiguration)

        val similarity = config.linkSpec.condition(instances.sourceInstance, instances.targetInstance, config.linkSpec.filter.threshold)

        if(similarity >= config.linkSpec.filter.threshold)
        {
            context.write(new Text(instances.sourceInstance.uri), new InstanceSimilarity(similarity, instances.targetInstance.uri))
        }
    }
}
