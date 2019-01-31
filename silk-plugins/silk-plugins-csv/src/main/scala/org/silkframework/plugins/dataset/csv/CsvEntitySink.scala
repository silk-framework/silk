package org.silkframework.plugins.dataset.csv

import org.silkframework.dataset.EntitySink
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.WritableResource

/**
 * Created by andreas on 12/11/15.
 */
class CsvEntitySink(file: WritableResource, settings: CsvSettings) extends CsvSink(file, settings) with EntitySink {

  override def writeEntity(subject: String, values: Seq[Seq[String]])
                          (implicit userContext: UserContext){
    write(values.map(_.mkString(settings.arraySeparator.getOrElse(' ').toString)))
  }
}
