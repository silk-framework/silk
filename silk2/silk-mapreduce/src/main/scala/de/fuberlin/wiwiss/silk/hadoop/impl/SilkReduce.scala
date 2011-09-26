package de.fuberlin.wiwiss.silk.hadoop.impl

import org.apache.hadoop.mapreduce.Reducer
import scala.collection.JavaConversions._
import org.apache.hadoop.io.Text
import de.fuberlin.wiwiss.silk.hadoop.SilkConfiguration

class SilkReduce extends Reducer[Text, EntityConfidence, Text, EntityConfidence] {

  protected override def reduce(sourceUri : Text, entitiySimilarities : java.lang.Iterable[EntityConfidence],
                                context : Reducer[Text, EntityConfidence, Text, EntityConfidence]#Context) {
    val config = SilkConfiguration.get(context.getConfiguration)

    config.linkSpec.filter.limit match {
      case Some(limit) => {
        for(entitySimilarity <- entitiySimilarities.take(limit)) {
          context.write(sourceUri, entitySimilarity)
        }
      }
      case None => {
        for(entitySimilarity <- entitiySimilarities) {
          context.write(sourceUri, entitySimilarity)
        }
      }
    }
  }
}
