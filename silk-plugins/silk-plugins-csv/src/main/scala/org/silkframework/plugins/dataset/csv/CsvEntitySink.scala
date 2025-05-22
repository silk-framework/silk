package org.silkframework.plugins.dataset.csv

import org.silkframework.dataset.EntitySink
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.WritableResource

/**
 * Created by andreas on 12/11/15.
 */
class CsvEntitySink(file: WritableResource, settings: CsvSettings) extends CsvSink(file, settings) with EntitySink {

  override def writeEntity(subject: String, values: IndexedSeq[Seq[String]])
                          (implicit userContext: UserContext): Unit = {
    // Concatenate multiple values per column (for performance reasons only if needed)
    val concatenatedValues =
      for(value <- values) yield {
        value.size match {
          case 0 => ""
          case 1 => value(0)
          case _ => value.mkString(settings.arraySeparator.getOrElse(' ').toString)
        }
      }
    write(concatenatedValues)
  }
}
