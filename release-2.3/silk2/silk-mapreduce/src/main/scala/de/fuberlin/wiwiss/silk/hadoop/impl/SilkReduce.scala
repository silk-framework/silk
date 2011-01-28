package de.fuberlin.wiwiss.silk.hadoop.impl

import org.apache.hadoop.mapreduce.Reducer
import scala.collection.JavaConversions._
import org.apache.hadoop.io.Text
import de.fuberlin.wiwiss.silk.hadoop.SilkConfiguration

class SilkReduce extends Reducer[Text, InstanceSimilarity, Text, InstanceSimilarity]
{
  protected override def reduce(sourceUri : Text, instanceSimilarities : java.lang.Iterable[InstanceSimilarity],
                                context : Reducer[Text, InstanceSimilarity, Text, InstanceSimilarity]#Context)
  {
    val config = SilkConfiguration.get(context.getConfiguration)

    config.linkSpec.filter.limit match
    {
      case Some(limit) =>
      {
        for(instanceSimilarity <- instanceSimilarities.take(limit))
        {
          context.write(sourceUri, instanceSimilarity)
        }
      }
      case None =>
      {
        for(instanceSimilarity <- instanceSimilarities)
        {
          context.write(sourceUri, instanceSimilarity)
        }
      }
    }
  }
}
