package org.silkframework.plugins.dataset.csv

import java.io.ByteArrayOutputStream

import org.silkframework.dataset.TypedProperty
import org.silkframework.entity.{StringValueType, ValueType}
import org.silkframework.runtime.resource.OutputStreamWritableResource

class CsvWriterTest extends CsvExecutorTest {

  override protected def write(settings: CsvSettings, headers: Seq[String], values: Seq[Seq[String]]): String = {
    val os = new ByteArrayOutputStream()
    val resource = OutputStreamWritableResource(os)

    val writer = new CsvWriter(resource, headers.map(str => TypedProperty(str, ValueType.STRING, isBackwardProperty = false)), settings)
    for(line <- values) {
      writer.writeLine(line)
    }
    writer.close()

    os.toString("UTF-8")
  }

}
