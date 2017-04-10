package org.silkframework.plugins.dataset.csv

import java.io.{InputStream, OutputStream, StringReader}
import java.time.Instant

import org.scalatest.{FlatSpec, Matchers}
import org.silkframework.entity.{EntitySchema, Path}
import org.silkframework.runtime.resource.{ClasspathResourceLoader, Resource, WritableResource}
import org.silkframework.util.Uri

class CsvSourceTest extends FlatSpec with Matchers {

  val resources = ClasspathResourceLoader("org/silkframework/plugins/dataset/csv")

  def writableResource(resource: Resource): WritableResource = {
    new WritableResource {
      override def write(write: (OutputStream) => Unit): Unit = ???

      override def name: String = resource.name

      override def path: String = resource.path

      override def exists: Boolean = resource.exists

      override def size: Option[Long] = resource.size

      override def modificationTime: Option[Instant] = resource.modificationTime

      override def load: InputStream = resource.load
    }
  }

  val settings =
    CsvSettings(
      separator = ',',
      quote = None,
      arraySeparator = None
    )

  val noSeparatorSettings = settings.copy(separator = ' ')

  val source = new CsvSource(resources.get("persons.csv"), settings)
  val datasetHard = CsvDataset(writableResource(resources.get("hard_to_parse.csv")), separator = "\t", quote = "")
  val emptyCsv = CsvDataset(writableResource(resources.get("empty.csv")), separator = "\t", quote = "")

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
    SeparatorDetector.detectSeparatorChar(
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

  "CsvDataset" should "detect separator on multi line instances and read entities accordingly" in {
    val autoConfigured = datasetHard.autoConfigured
    autoConfigured.separator shouldBe ","
    val source = autoConfigured.source
    val paths = source.retrievePaths(Uri("")).toIndexedSeq.map(_.asStringTypedPath)
    val entities = source.retrieve(EntitySchema(Uri(""), paths)).toSeq
    entities.size shouldBe 3
    val multilineEntity = entities.drop(1).head
    val expectedValue = """Markus from
                          |the other company,
                          |who does not like pizza""".stripMargin
    multilineEntity.values.drop(1).head.head shouldBe expectedValue
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
}
