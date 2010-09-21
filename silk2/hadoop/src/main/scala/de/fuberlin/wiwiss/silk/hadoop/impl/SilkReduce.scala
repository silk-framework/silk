package de.fuberlin.wiwiss.silk.hadoop.impl

import org.apache.hadoop.mapreduce.Reducer
import scala.collection.JavaConversions._
import org.apache.hadoop.io.Text

class SilkReduce extends Reducer[Text, InstanceSimilarity, Text, InstanceSimilarity]
{
    protected override def reduce(sourceUri : Text, instanceSimilarities : java.lang.Iterable[InstanceSimilarity],
                                  context : Reducer[Text, InstanceSimilarity, Text, InstanceSimilarity]#Context)
    {
        //TODO consider link limit
        for(instanceSimilarity <- instanceSimilarities)
        {
            context.write(sourceUri, instanceSimilarity)
        }
    }
}