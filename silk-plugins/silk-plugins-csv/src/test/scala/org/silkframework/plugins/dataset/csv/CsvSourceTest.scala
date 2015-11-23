package org.silkframework.plugins.dataset.csv

import org.silkframework.entity.Path
import org.silkframework.entity.rdf.SparqlEntitySchema
import org.silkframework.runtime.resource.ClasspathResourceLoader
import org.scalatest.{Matchers, FlatSpec}

class CsvSourceTest extends FlatSpec with Matchers {

  val resources = new ClasspathResourceLoader("de/fuberlin/wiwiss/silk/plugins/dataset/csv")

  val settings =
    CsvSettings(
      separator = ',',
      quote = None,
      arraySeparator = None
    )
  
  val noSeparatorSettings = settings.copy(separator = ' ')

  val source = new CsvSource(resources.get("persons.csv"), settings)

  "For persons.csv, CsvParser" should "extract the schema" in {
    val properties = source.retrieveSparqlPaths().map(_._1.propertyUri.get).toSet
    properties should equal (Set("ID", "Name", "Age"))
  }

  "For persons.csv, CsvParser" should "extract all columns" in {
    val entityDesc = SparqlEntitySchema(paths = IndexedSeq(Path("ID"), Path("Name"), Path("Age")))
    val persons = source.retrieveSparqlEntities(entityDesc).toIndexedSeq
    persons(0).values should equal (IndexedSeq(Set("1"), Set("Max Mustermann"), Set("30")))
    persons(1).values should equal (IndexedSeq(Set("2"), Set("Markus G."), Set("24")))
    persons(2).values should equal (IndexedSeq(Set("3"), Set("John Doe"), Set("55")))
  }

  "For persons.csv, CsvParser" should "extract selected columns" in {
    val entityDesc = SparqlEntitySchema(paths = IndexedSeq(Path("Name"), Path("Age")))
    val persons = source.retrieveSparqlEntities(entityDesc).toIndexedSeq
    persons(0).values should equal (IndexedSeq(Set("Max Mustermann"), Set("30")))
    persons(1).values should equal (IndexedSeq(Set("Markus G."), Set("24")))
    persons(2).values should equal (IndexedSeq(Set("John Doe"), Set("55")))
  }
  
  "SeparatorDetector" should "detect comma separator" in {
    SeparatorDetector.detectSeparatorCharInLines(
      Seq(
        """f1,f2,f3""",
        """1,"test",3""",
        """1,"test, with, commas, in, literal",3"""
      ),
      noSeparatorSettings
    ) shouldBe Some(DetectedSeparator(',', 3, 0))
  }

  "SeparatorDetector" should "detect tab separator" in {
    val tab = "\t"
    SeparatorDetector.detectSeparatorCharInLines(
      Seq(
        s"""f1${tab}f2${tab}f3""",
        s"""1${tab}"test"${tab}3""",
        s"""1${tab}"test, with, commas, in, literal"${tab}3"""
      ),
      noSeparatorSettings
    ) shouldBe Some(DetectedSeparator('\t', 3, 0))
  }

  "SeparatorDetector" should "detect lines to skip" in {
    val tab = "\t"
    val validLines = for(i <- 1 to 100) yield {
      s"""1,2,3"""
    }
    SeparatorDetector.detectSeparatorCharInLines(
      Seq(
        s"""Some gibberish""",
        s""""""
      ) ++ validLines,
      noSeparatorSettings
    ) shouldBe Some(DetectedSeparator(',', 3, 2))
  }

  "SeparatorDetector" should "return None if not confident enough" in {
    val tab = "\t"
    SeparatorDetector.detectSeparatorCharInLines(
      Seq(
        s"""f1${tab}f2${tab}f3""",
        s"""1${tab}"test"""",
        s"""1,"test, with, commas, in, literal",3"""
      ),
      noSeparatorSettings
    ) shouldBe None
  }
}
