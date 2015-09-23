package de.fuberlin.wiwiss.silk.plugins.dataset.csv

import de.fuberlin.wiwiss.silk.entity.{Path, EntityDescription}
import de.fuberlin.wiwiss.silk.runtime.resource.ClasspathResourceLoader
import org.scalatest.{Matchers, FlatSpec}

class CsvSourceTest extends FlatSpec with Matchers {

  val resources = new ClasspathResourceLoader("de/fuberlin/wiwiss/silk/plugins/dataset/csv")

  val settings =
    CsvSettings(
      separator = ',',
      quote = None,
      arraySeparator = None
    )

  val source = new CsvSource(resources.get("persons.csv"), settings)

  "For persons.csv, CsvParser" should "extract the schema" in {
    val properties = source.retrievePaths().map(_._1.propertyUri.get).toSet
    properties should equal (Set("ID", "Name", "Age"))
  }

  "For persons.csv, CsvParser" should "extract all columns" in {
    val entityDesc = EntityDescription(paths = IndexedSeq(Path("ID"), Path("Name"), Path("Age")))
    val persons = source.retrieve(entityDesc).toIndexedSeq
    persons(0).values should equal (IndexedSeq(Set("1"), Set("Max Mustermann"), Set("30")))
    persons(1).values should equal (IndexedSeq(Set("2"), Set("Markus G."), Set("24")))
    persons(2).values should equal (IndexedSeq(Set("3"), Set("John Doe"), Set("55")))
  }

  "For persons.csv, CsvParser" should "extract selected columns" in {
    val entityDesc = EntityDescription(paths = IndexedSeq(Path("Name"), Path("Age")))
    val persons = source.retrieve(entityDesc).toIndexedSeq
    persons(0).values should equal (IndexedSeq(Set("Max Mustermann"), Set("30")))
    persons(1).values should equal (IndexedSeq(Set("Markus G."), Set("24")))
    persons(2).values should equal (IndexedSeq(Set("John Doe"), Set("55")))
  }

}
