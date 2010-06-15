package de.fuberlin.wiwiss.silk.hadoop

import org.apache.hadoop.mapreduce.Mapper
import org.apache.hadoop.io.{NullWritable, Text}

class SilkMap extends Mapper[NullWritable, InstancePair, Text, InstanceSimilarity]
{
    protected override def map(key : NullWritable, instances : InstancePair, context : Mapper[NullWritable, InstancePair, Text, InstanceSimilarity]#Context)
    {
        for(similarity <- Silk.linkSpec.condition.evaluate(instances.sourceInstance, instances.targetInstance)
            if similarity >= Silk.linkSpec.filter.threshold)
        {
            context.write(new Text(instances.sourceInstance.uri), new InstanceSimilarity(similarity, instances.targetInstance.uri))
        }
    }
}
