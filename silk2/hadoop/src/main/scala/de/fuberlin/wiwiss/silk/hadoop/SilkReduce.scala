package de.fuberlin.wiwiss.silk.hadoop

import org.apache.hadoop.mapreduce.Reducer
import scala.collection.JavaConversions._
import org.apache.hadoop.io.{NullWritable, Text}

class SilkReduce extends Reducer[Text, InstanceSimilarity, Text, InstanceSimilarity]
{
    protected override def reduce(sourceUri : Text, instanceSimilarities : java.lang.Iterable[InstanceSimilarity],
                                  context : Reducer[Text, InstanceSimilarity, Text, InstanceSimilarity]#Context)
    {
        for(instanceSimilarity <- instanceSimilarities)
        {
            context.write(sourceUri, instanceSimilarity)
        }
    }
}