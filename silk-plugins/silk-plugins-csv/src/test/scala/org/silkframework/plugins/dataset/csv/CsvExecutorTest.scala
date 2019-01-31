package org.silkframework.plugins.dataset.csv

import org.scalatest.{FlatSpec, MustMatchers}

/**
  * Base class for testing CSV execution.
  * Should be subclassed for each execution type.
  */
abstract class CsvExecutorTest extends FlatSpec with MustMatchers {

  behavior of getClass.getSimpleName.stripSuffix("Test")

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

    result mustBe expected
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

    result mustBe expected
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

    result mustBe expected
  }

  it should "write columns that contain URIs" in {
    val result =
      write(
        settings = CsvSettings(),
        headers = Seq("http://example.org/firstName", "http://example.org/lastName"),
        values = Seq(Seq("John", "Doe"), Seq("Max", "Ford"))
      )

    val expected =
      "http://example.org/firstName,http://example.org/lastName\n" +
        "John,Doe\n" +
        "Max,Ford\n"

    result mustBe expected
  }

  protected def write(settings: CsvSettings, headers: Seq[String], values: Seq[Seq[String]]): String

}


