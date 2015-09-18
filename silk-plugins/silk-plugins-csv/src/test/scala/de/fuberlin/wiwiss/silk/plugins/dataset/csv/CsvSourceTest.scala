package de.fuberlin.wiwiss.silk.plugins.dataset.csv

import de.fuberlin.wiwiss.silk.runtime.resource.ClasspathResourceLoader
import org.scalatest.{Matchers, FlatSpec}

class CsvSourceTest extends FlatSpec with Matchers {

  val resources = new ClasspathResourceLoader("de/fuberlin/wiwiss/silk/plugins/dataset/csv")

  val settings =
    CsvSettings(
      separator = ',',
      arraySeparator = ""
    )

  val source = new CsvSource(resources.get("persons.csv"), settings)

  "For persons.csv, CsvParser" should "extract the schema" in {
    val properties = source.retrievePaths().map(_._1.propertyUri.get).toSet
    properties should equal (Set("ID", "Name", "Age"))
  }

}
