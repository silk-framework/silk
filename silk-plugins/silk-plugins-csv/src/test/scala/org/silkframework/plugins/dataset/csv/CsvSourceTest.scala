package org.silkframework.plugins.dataset.csv

import java.io.StringReader

import org.scalatest.{FlatSpec, Matchers}
import org.silkframework.dataset.DataSource
import org.silkframework.entity.{Entity, EntitySchema, Path}
import org.silkframework.runtime.resource.{ClasspathResourceLoader, ReadOnlyResource}
import org.silkframework.util.Uri

class CsvSourceTest extends FlatSpec with Matchers {

  val resources = ClasspathResourceLoader("org/silkframework/plugins/dataset/csv")

  val settings =
    CsvSettings(
      separator = ',',
      quote = None,
      arraySeparator = None
    )

  val noSeparatorSettings = settings.copy(separator = ' ')

  val source = new CsvSource(resources.get("persons.csv"), settings)
  val emptyHeaderFieldsDataset = new CsvSource(resources.get("emptyHeaderFields.csv"), settings)
  val datasetHard = CsvDataset(ReadOnlyResource(resources.get("hard_to_parse.csv")), separator = "\t", quote = "")
  val emptyCsv = CsvDataset(ReadOnlyResource(resources.get("empty.csv")), separator = "\t", quote = "")
  val tabSeparated = CsvDataset(ReadOnlyResource(resources.get("tab_separated.csv")), separator = "\\t")
  val tabArraySeparated = CsvDataset(ReadOnlyResource(resources.get("tab_array_separated.csv")), arraySeparator = "\t")

  "For persons.csv, CsvParser" should "extract the schema" in {
    val properties = source.retrievePaths("").map(_.propertyUri.get.toString).toSet
    properties should equal(Set("ID", "Name", "Age"))
  }

  "For persons.csv, CsvParser" should "extract all columns" in {
    val entityDesc = EntitySchema(typeUri = Uri(""), typedPaths = IndexedSeq(Path("ID").asStringTypedPath, Path("Name").asStringTypedPath, Path("Age").asStringTypedPath))
    val persons = source.retrieve(entityDesc).toIndexedSeq
    persons(0).values should equal(IndexedSeq(Seq("1"), Seq("Max Mustermann"), Seq("30")))
    persons(1).values should equal(IndexedSeq(Seq("2"), Seq("Markus G."), Seq("24")))
    persons(2).values should equal(IndexedSeq(Seq("3"), Seq("John Doe"), Seq("55")))
  }

  "For persons.csv, CsvParser" should "extract selected columns" in {
    val entityDesc = EntitySchema(typeUri = Uri(""), typedPaths = IndexedSeq(Path("Name").asStringTypedPath, Path("Age").asStringTypedPath))
    val persons = source.retrieve(entityDesc).toIndexedSeq
    persons(0).values should equal(IndexedSeq(Seq("Max Mustermann"), Seq("30")))
    persons(1).values should equal(IndexedSeq(Seq("Markus G."), Seq("24")))
    persons(2).values should equal(IndexedSeq(Seq("John Doe"), Seq("55")))
  }

  "SeparatorDetector" should "detect comma separator" in {
    detect(Seq(
        """f1,f2,f3""",
        """1,"test",3""",
        """1,"test, with, commas, in, literal",3"""
      )) shouldBe Some(DetectedSeparator(',', 3, 0))
  }

  it should "detect tab separator" in {
    val tab = "\t"
    detect(Seq(
      s"""f1${tab}f2${tab}f3""",
      s"""1${tab}"test"${tab}3""",
      s"""1${tab}"test, with, commas, in, literal"${tab}3"""
    )) shouldBe Some(DetectedSeparator('\t', 3, 0))
  }

  it should "detect hash separator" in {
    val hash = "#"
    detect(Seq(
      s"""f1${hash}f2${hash}f3""",
      s"""1${hash}"test"${hash}3""",
      s"""1${hash}"test, with, commas, in, literal"${hash}3"""
    )) shouldBe Some(DetectedSeparator('#', 3, 0))
  }

  it should "detect lines to skip" in {
    val tab = "\t"
    val validLines = for (i <- 1 to 100) yield {
      s"""1,2,3"""
    }
    detect(Seq(
      s"""Some gibberish""",
      s"""""", // Empty lines are skipped automatically
      s"""more gibberish"""
    ) ++ validLines) shouldBe Some(DetectedSeparator(',', 3, 2))
  }

  private def detect(lines: Seq[String]): Option[DetectedSeparator] = {
    CsvSeparatorDetector.detectSeparatorChar(
      new StringReader(lines.mkString("\n")),
      noSeparatorSettings,
      lines.size
    )
  }

  it should "return None if not confident enough" in {
    val tab = "\t"
    detect(Seq(
      s"""f1${tab}f2${tab}f3""",
      s"""1${tab}"test"""",
      s"""1,"test, with, commas, in, literal",3"""
    )) shouldBe None
  }

  it should "interpret escaped separators" in {
    val source = tabSeparated.source
    val entities: Seq[Entity] = getEntities(source)
    entities.size shouldBe 2
    entities.map(_.values.flatten.head) shouldBe Seq("value1", "abc")
    tabSeparated.autoConfigured.separator shouldBe "\\t"
  }

  it should "accept tab as array separator char" in {
    for(source <- Seq(tabArraySeparated.source, tabArraySeparated.copy(arraySeparator = "\\t").source)) {
      val entities: Seq[Entity] = getEntities(source)
      entities.size shouldBe 2
      entities.head.values shouldBe IndexedSeq(Seq("val1a", "val1b"), Seq("val2a", "val2b"))
      tabSeparated.autoConfigured.separator shouldBe "\\t"
    }
  }

  "CsvDataset" should "detect separator on multi line instances and read entities accordingly" in {
    val autoConfigured = datasetHard.autoConfigured
    autoConfigured.separator shouldBe ","
    val source = autoConfigured.source
    val entities: Seq[Entity] = getEntities(source)
    entities.size shouldBe 3
    val multilineEntity = entities.drop(1).head
    val expectedValue = "Markus from\n" +
                        "the other company,\n" +
                        "who does not like pizza"
    multilineEntity.values.drop(1).head.head shouldBe expectedValue
  }

  private def getEntities(dataSource: DataSource): Seq[Entity] = {
    val paths = dataSource.retrievePaths(Uri("")).toIndexedSeq.map(_.asStringTypedPath)
    val entities = dataSource.retrieve(EntitySchema(Uri(""), paths)).toSeq
    entities
  }

  it should "not fail when auto-configuring on empty CSV files" in {
    val autoConfigured = emptyCsv.autoConfigured
    autoConfigured.separator shouldBe "\\t"
  }

  "CsvSourceHelper" should "escape and unescape standard fields correctly" in {
    val input = """I said: "What, It escaped?""""
    val line = CsvSourceHelper.serialize(Seq(input, input, input))
    val back = CsvSourceHelper.parse(line)
    for (str <- back) {
      str shouldBe input
    }
    val normal = CsvSourceHelper.serialize(Seq("Just a normal string", "and, not normal"))
    normal shouldBe "Just a normal string,\"and, not normal\""
  }

  "CsvDataset" should "generate sane column names when they are empty" in {
    emptyHeaderFieldsDataset.propertyList shouldBe Seq(
      "unnamed_col1","field2","unnamed_col3_2","unnamed_col3","unnamed_col6_3","unnamed_col6_4","unnamed_col7"
    )
  }
}
