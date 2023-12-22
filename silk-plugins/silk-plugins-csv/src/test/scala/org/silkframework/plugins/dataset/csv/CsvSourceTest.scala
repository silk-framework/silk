package org.silkframework.plugins.dataset.csv


import org.silkframework.config.Prefixes
import org.silkframework.dataset.{DataSource, DatasetSpec}
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.entity.{Entity, EntitySchema, ValueType}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.{PluginContext, TestPluginContext}
import org.silkframework.runtime.resource._
import org.silkframework.util.Uri

import java.io.StringReader
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class CsvSourceTest extends AnyFlatSpec with Matchers {

  behavior of "CSV Source"

  val resources: ResourceManager = ReadOnlyResourceManager(ClasspathResourceLoader("org/silkframework/plugins/dataset/csv"))

  implicit val userContext: UserContext = UserContext.Empty
  implicit val prefixes: Prefixes = Prefixes.empty
  implicit val pluginContext: PluginContext = TestPluginContext(prefixes, resources)

  val settings =
    CsvSettings(
      quote = None,
      arraySeparator = None
    )

  val noSeparatorSettings: CsvSettings = settings.copy(separator = ' ')

  lazy val source = new CsvSource(resources.get("persons.csv"), settings)
  lazy val emptyHeaderFieldsDataset = new CsvSource(resources.get("emptyHeaderFields.csv"), settings)
  lazy val dirtyHeaders = new CsvSource(resources.get("dirtyHeaders.csv"), settings)
  lazy val datasetHard = CsvDataset(ReadOnlyResource(resources.get("hard_to_parse.csv")), separator = "\t", quote = "")
  lazy val emptyCsv = CsvDataset(ReadOnlyResource(resources.get("empty.csv")), separator = "\t", quote = "")
  lazy val tabSeparated = CsvDataset(ReadOnlyResource(resources.get("tab_separated.csv")), separator = "\\t")
  lazy val tabArraySeparated = CsvDataset(ReadOnlyResource(resources.get("tab_array_separated.csv")), arraySeparator = "\t")
  lazy val noHeaders = CsvDataset(ReadOnlyResource(resources.get("no_header.csv")), properties = "vals1,vals2,vals3")
  lazy val iso8859 = CsvDataset(ReadOnlyResource(resources.get("iso8859.csv")))
  lazy val windows1255 = CsvDataset(ReadOnlyResource(resources.get("windows-1255.csv")))
  lazy val nonStandard = CsvDataset(ReadOnlyResource(resources.get("nonStandard.csv")))
  lazy val cmem4065withProperties = new CsvSource(resources.get("cmem-4065.csv"), settings, properties = "A/B,urn:prop:propA,https://test.com/some/valid?uri=true")
  lazy val cmem4065 = new CsvSource(resources.get("cmem-4065.csv"), settings)

  "For persons.csv, CsvParser" should "extract the schema" in {
    val properties = source.retrievePaths("").map(_.propertyUri.get.toString).toSet
    properties should equal(Set("ID", "Name", "Age"))
  }

  it should "type all source paths as value typed paths" in {
    source.retrievePaths("") map (tp => tp.toUntypedPath.normalizedSerialization -> tp.valueType) shouldBe IndexedSeq(
      "ID" -> ValueType.STRING,
      "Name" -> ValueType.STRING,
      "Age" -> ValueType.STRING
    )
  }

  "For dirtyHeaders.csv, CsvParser" should "encode the column names when extracting the schema" in {
    val properties = dirtyHeaders.retrievePaths("").map(_.propertyUri.get.toString).toSet
    properties should equal(Set("ID+mit+Sonderzeichen%25%21%3F", "Name+of+the+Person", "Alter%C3%84%C3%96%C3%9C"))
  }

  "For persons.csv, CsvParser" should "extract all columns" in {
    val entityDesc = EntitySchema(typeUri = Uri(""), typedPaths = IndexedSeq(UntypedPath("ID").asStringTypedPath, UntypedPath("Name").asStringTypedPath, UntypedPath("Age").asStringTypedPath))
    val persons = source.retrieve(entityDesc).entities.toIndexedSeq
    persons(0).values should equal(IndexedSeq(Seq("1"), Seq("Max Mustermann"), Seq("30")))
    persons(1).values should equal(IndexedSeq(Seq("2"), Seq("Markus G."), Seq("24")))
    persons(2).values should equal(IndexedSeq(Seq("3"), Seq("John Doe"), Seq("55")))
  }

  "For persons.csv, CsvParser" should "extract selected columns" in {
    val entityDesc = EntitySchema(typeUri = Uri(""), typedPaths = IndexedSeq(UntypedPath("Name").asStringTypedPath, UntypedPath("Age").asStringTypedPath))
    val persons = source.retrieveEntities(entityDesc).entities.toIndexedSeq
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

  "DatasetSpec" should "not cache the CSV source" in {
    val manager = InMemoryResourceManager()
    val resource = manager.get("empty.csv")
    val csvDataset = CsvDataset(resource)
    val datasetSpec = DatasetSpec(csvDataset)
    val emptyPaths = datasetSpec.source.retrievePaths("")
    emptyPaths shouldBe empty
    resource.writeString("id,name\n1,doe")
    val nonEmptyPaths = datasetSpec.source.retrievePaths("")
    nonEmptyPaths.size shouldBe 2
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

  it should "detect windows-1252 (similar to ISO-8859-1) encoding" in {
    // The detection library assumes windows-1252 even though iso-8859-1 would also be correct.
    val autoConfigured = iso8859.autoConfigured
    autoConfigured.charset shouldBe "windows-1252"
  }

  it should "detect windows-1255 encoding" in {
    val autoConfigured = windows1255.autoConfigured
    autoConfigured.charset shouldBe "windows-1255"
  }

  it should "auto-configure random example correctly" in {
    val autoConfigured = nonStandard.autoConfigured
    autoConfigured.separator shouldBe ";"
    autoConfigured.source.retrievePaths("").map(_.normalizedSerialization) shouldBe Seq("id", "label")
    autoConfigured.linesToSkip shouldBe 3
    autoConfigured.source.retrieve(EntitySchema("", IndexedSeq(UntypedPath.parse("id").asStringTypedPath)))
      .headOption.toSeq
      .flatMap(_.values.flatten) shouldBe Seq("1")
  }

  private def getEntities(dataSource: DataSource): Seq[Entity] = {
    val paths = dataSource.retrievePaths(Uri("")).toIndexedSeq
    val entities = dataSource.retrieve(EntitySchema(Uri(""), paths)).entities.toSeq
    entities
  }

  it should "not fail when auto-configuring on empty CSV files" in {
    val autoConfigured = emptyCsv.autoConfigured
    autoConfigured.separator shouldBe "\\t"
  }

  it should "skip header detection when properties are provided" in {
    val source = noHeaders.source
    val entities: Seq[Entity] = getEntities(source)
    entities.size shouldBe 4                        //in this case number of entities is the same as number of lines in csv
    val top = entities.head
    top.schema.propertyNames shouldBe IndexedSeq("vals1", "vals2", "vals3")
    top.valueOfPath(UntypedPath("vals2")).head shouldBe "val2"
  }

  it should "encode manually provided properties, if necessary" in {
    val source = noHeaders.copy(properties = "path with spaces,path+already%20encoded,path3").source
    val entities: Seq[Entity] = getEntities(source)
    val top = entities.head
    top.schema.propertyNames shouldBe IndexedSeq("path+with+spaces", "path+already%20encoded", "path3")
    top.valueOfPath(UntypedPath("path+with+spaces")).head shouldBe "val1"
    top.valueOfPath(UntypedPath("path+already%20encoded")).head shouldBe "val2"
  }

  it should "support #idx special forward path" in {
    val s = source
    val schema = EntitySchema("", typedPaths = IndexedSeq(UntypedPath("#idx").asStringTypedPath))
    val entities = s.retrieve(schema, limitOpt = Some(3)).entities
    entities.map(_.values.flatten.head).toSeq shouldBe Seq("1", "2", "3")
  }

  it should "support #line and #column special forward path" in {
    val s = source
    val schema = EntitySchema("", typedPaths = IndexedSeq(UntypedPath("#line").asStringTypedPath, UntypedPath.parse("Name/#column").asStringTypedPath))
    val entities = s.retrieve(schema, limitOpt = Some(3)).entities
    entities.map(_.values.map(_.head)).toSeq shouldBe Seq(Seq("1", "1"), Seq("2", "1"), Seq("3", "1"))
  }

  it should "respect the limit parameter" in {
    val s = source
    val schema = EntitySchema("", typedPaths = IndexedSeq(UntypedPath("Name").asStringTypedPath))
    val entities = s.retrieve(schema, limitOpt = Some(2)).entities
    entities should have size 2
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

  "Csv Source" should "fetch entities by URI" in {
    val es = EntitySchema("", IndexedSeq())
    val entities = source.retrieve(es).entities.toSeq
    entities.size shouldBe 3
    source.retrieveByUri(es, Seq(entities.head.uri)).entities.size shouldBe 1
  }

  it should "convert URI conform property (parameter) names B to a form that is consistent with the CSV attribute conversion" in {
    cmem4065withProperties.retrievePaths("").map(_.normalizedSerialization) shouldBe IndexedSeq("A%2FB", "<urn:prop:propA>", "<https://test.com/some/valid?uri=true>")
    cmem4065.retrievePaths("").map(_.normalizedSerialization) shouldBe IndexedSeq("A%2FB", "<urn:prop:propA>", "<https://test.com/some/valid?uri=true>")
  }
}
