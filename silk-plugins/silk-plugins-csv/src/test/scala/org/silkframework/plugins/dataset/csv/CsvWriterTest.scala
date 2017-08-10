package org.silkframework.plugins.dataset.csv

import java.io.ByteArrayOutputStream

import org.scalatest.{FlatSpec, Matchers}
import org.silkframework.dataset.TypedProperty
import org.silkframework.entity.StringValueType
import org.silkframework.runtime.resource.OutputStreamWritableResource

class CsvWriterTest extends FlatSpec with Matchers {

  behavior of "CsvWriter"

  it should "write rows in default CSV format (i.e., comma-separated values)" in {
    val result =
      write(
        settings = CsvSettings(),
        headers = Seq("FirstName", "LastName"),
        values = Seq(Seq("John", "Doe"), Seq("Max", "Ford"))
      )

    val expected =
      "FirstName,LastName\n" +
      "John,Doe\n" +
      "Max,Ford\n"

    result shouldBe expected
  }

  it should "write rows in TSV format (i.e., tab-separated values)" in {
    val sep = '\t'
    val result =
      write(
        settings = CsvSettings(separator = sep),
        headers = Seq("FirstName", "LastName"),
        values = Seq(Seq("John", "Doe"), Seq("Max", "Ford"))
      )

    val expected =
      s"FirstName${sep}LastName\n" +
      s"John${sep}Doe\n" +
      s"Max${sep}Ford\n"

    result shouldBe expected
  }

  it should "quote values that contain the separator" in {
    val result =
      write(
        settings = CsvSettings(quote = Some('"')),
        headers = Seq("FirstName", "LastName"),
        values = Seq(Seq("John,Max", "Doe"))
      )

    val expected =
      "FirstName,LastName\n" +
       "\"John,Max\",Doe\n"

    result shouldBe expected
  }

  private def write(settings: CsvSettings, headers: Seq[String], values: Seq[Seq[String]]): String = {
    val os = new ByteArrayOutputStream()
    val resource = OutputStreamWritableResource(os)

    val writer = new CsvWriter(resource, headers.map(str => TypedProperty(str, StringValueType, isBackwardProperty = false)), settings)
    for(line <- values) {
      writer.writeLine(line)
    }
    writer.close()

    os.toString("UTF-8")
  }

}
