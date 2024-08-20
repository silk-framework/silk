package org.silkframework.plugins.dataset.json


import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.silkframework.config.Prefixes
import org.silkframework.dataset._
import org.silkframework.entity.paths.{TypedPath, UntypedPath}
import org.silkframework.entity._
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource._
import org.silkframework.util.Uri

import scala.collection.mutable

abstract class JsonSourceTest extends AnyFlatSpec with Matchers {

  implicit val userContext: UserContext = UserContext.Empty
  implicit val prefixes: Prefixes = Prefixes.empty

  protected val resources: ResourceLoader = ClasspathResourceLoader("org/silkframework/plugins/dataset/json/")

  protected def createSource(resource: Resource, basePath: String, uriPattern: String): JsonSource
  protected def createDataset(resource: WritableResource): JsonDataset

  protected def jsonExampleSource: JsonSource = {
    val source = createSource(resources.get("example.json"), "", "#id")
    source
  }

  protected def jsonLinesSource: JsonSource = {
    createSource(resources.get("exampleLines.jsonl"), "", "#id")
  }

  it should "collect all paths" in {
    val paths = jsonExampleSource.collectPaths(Int.MaxValue)
    paths.map(_.mkString("/")) mustBe
      Seq("", "persons", "persons/id", "persons/name", "persons/phoneNumbers", "persons/phoneNumbers/type",
        "persons/phoneNumbers/number", "organizations", "organizations/name")
  }

  it should "return all inner node types" in {
    val types = jsonExampleSource.retrieveTypes().map(_._1).toSet
    types mustBe Set(
      "",
      "persons",
      "persons/phoneNumbers",
      "organizations"
    )
  }

  it should "not return an entity for an empty JSON array" in {
    val resourceManager = InMemoryResourceManager()
    val resource = resourceManager.get("test.json")
    resource.writeString(
      """
        |{"data":[]}
      """.stripMargin)
    val source = createSource(resource, "data", "http://blah")
    val entities = source.retrieve(EntitySchema.empty).entities
    entities mustBe empty
  }

  it should "not return entities for valid paths" in {
    val resourceManager = InMemoryResourceManager()
    val resource = resourceManager.get("test.json")
    resource.writeString(
      """
        |{"data":{"entities":[{"id":"ID"}]}}
      """.stripMargin)
    val source = createSource(resource, "data/entities", "http://blah/{id}")
    val entities = source.retrieve(EntitySchema.empty).entities.toSeq
    entities.size mustBe 1
    entities.head.uri.toString mustBe "http://blah/ID"
  }

  it should "return peak results" in {
    val result = jsonExampleSource.peak(EntitySchema(Uri(""), typedPaths = IndexedSeq(UntypedPath.parse("/persons/phoneNumbers/number").asStringTypedPath)), 3).toSeq
    result.size mustBe 1
    result.head.values mustBe IndexedSeq(Seq("123", "456", "789"))
  }

  it should "return peak results with sub path set" in {
    val result = jsonExampleSource.peak(EntitySchema(Uri(""), typedPaths = IndexedSeq(UntypedPath.parse("/number").asStringTypedPath),
      subPath = UntypedPath.parse("/persons/phoneNumbers")), 3).toSeq
    result.map(_.values) mustBe Seq(IndexedSeq(Seq("123")), IndexedSeq(Seq("456")), IndexedSeq(Seq("789")))
  }

  it should "return all paths including intermediate paths for retrieve paths" in {
    val paths = jsonExampleSource.retrievePaths(Uri(""), depth = Int.MaxValue)
    paths.size mustBe 8
    paths must contain allOf(TypedPath("/persons", ValueType.URI), TypedPath("/persons/phoneNumbers", ValueType.URI))
  }

  it should "return all paths of depth 1" in {
    val paths = jsonExampleSource.retrievePaths(Uri(""), depth = 1)
    paths.map(_.toUntypedPath.normalizedSerialization) mustBe Seq("persons", "organizations")
  }

  it should "return all paths of depth 2" in {
    val paths = jsonExampleSource.retrievePaths(Uri(""), depth = 2)
    paths.map(_.toUntypedPath.normalizedSerialization) mustBe Seq("persons", "persons/id", "persons/name", "persons/phoneNumbers", "organizations", "organizations/name")
  }

  it should "return all paths of depth 1 of sub path" in {
    val paths = jsonExampleSource.retrievePaths(Uri("/persons"), depth = 1)
    paths.map(_.toUntypedPath.normalizedSerialization) mustBe Seq("id", "name", "phoneNumbers")
  }

  it should "return all paths of depth 2 of sub path" in {
    val paths = jsonExampleSource.retrievePaths(Uri("/persons"), depth = 2)
    paths.map(_.toUntypedPath.normalizedSerialization) mustBe Seq("id", "name", "phoneNumbers", "phoneNumbers/type", "phoneNumbers/number")
  }

  it should "return all paths of depth 1 of sub path of length 2" in {
    val paths = jsonExampleSource.retrievePaths(Uri("/persons/phoneNumbers"), depth = 1)
    paths.map(_.toUntypedPath.normalizedSerialization) mustBe Seq("type", "number")
  }

  it should "return max. limit paths for retrievePaths" in {
    jsonExampleSource.retrievePaths(Uri(""), limit = Some(3)) must have size 3
  }

  it should "return max. limit paths for retrieveTypes" in {
    jsonExampleSource.retrieveTypes(limit = Some(2)) must have size 2
  }

  it should "return valid URIs for resource paths" in {
    val result = jsonExampleSource.retrieve(EntitySchema(Uri(""), typedPaths = IndexedSeq(UntypedPath.parse("/persons").asStringTypedPath))).entities
    val uris = result.flatMap(_.values.flatten).toSeq
    for(uri <- uris) {
      assert(Uri(uri).isValidUri, s"URI $uri was not valid!")
    }
    uris.distinct.size mustBe uris.size
  }

  private val jsonWithNull = """{"values": ["val", null]}"""

  it should "return JSON null values as missing values" in {
    val source: DataSource = jsonSource(jsonWithNull)
    val entities = source.retrieve(EntitySchema("", typedPaths = IndexedSeq(UntypedPath("values").asStringTypedPath))).entities
    entities.map(_.values).toSeq mustBe Seq(Seq(Seq("val")))
  }

  private val jsonWithNullObject =
    """{"objects": [
      |  {"value":"val", "nestedObject": {"nestedValue": "nested"}},
      |  null,
      |  {"value":"val2"}
      |]}""".stripMargin

  it should "be able to ignore null JSON objects in the middle of longer paths" in {
    val source: DataSource = jsonSource(jsonWithNullObject)

    val entities = source.retrieve(EntitySchema("", typedPaths = IndexedSeq(UntypedPath.parse("objects/value").asStringTypedPath))).entities
    entities.map(_.values).toSeq mustBe Seq(Seq(Seq("val", "val2")))
  }

  private val jsonComplex =
    """{ "object": {"blah": 3},
      |  "objects": [
      |    {"value":"val", "nestedObject": {"nestedValue": "nested"}},
      |    null,
      |    {"value": "val2", "boolean": true, "int": 3, "float": 3.41, "emptyObject": {}, "emptyArray": [], "array": [1,2,3], "objectArray": [{"v": 2}]}
      |  ],
      |  "values": ["arr1", "arr2"]
      |}""".stripMargin

  it should "collect paths via streaming" in {
    val source = jsonSource(jsonComplex)
    val paths = source.collectPaths(limit = Int.MaxValue)
    paths.map(_.mkString("/")) mustBe Seq("", "object", "object/blah", "objects", "objects/value", "objects/nestedObject", "objects/nestedObject/nestedValue",
      "objects/boolean", "objects/int", "objects/float", "objects/emptyObject", "objects/emptyArray", "objects/array",
      "objects/objectArray", "objects/objectArray/v", "values")
  }

  it should "collect limited paths via streaming" in {
    val source = jsonSource(jsonComplex)
    val paths = source.collectPaths(limit = 5)
    paths.map(_.mkString("/")) mustBe Seq("", "object", "object/blah", "objects", "objects/value")
  }

  it should "collect values of path" in {
    val pathValues = mutable.HashMap[List[String], mutable.HashSet[String]]()
    val collectValues: (List[String], String) => Unit = (path, value) => {
      val values = pathValues.getOrElseUpdate(path, mutable.HashSet.empty[String])
      values.add(value)
    }
    val source = jsonSource(jsonComplex)
    source.collectPaths(Int.MaxValue, collectValues)
    pathValues.map{case (k, v) => (k.reverse.mkString("/"), v)} mustBe Map(
      "objects/array" -> Set("1", "2", "3"),
      "object/blah" -> Set("3"),
      "objects/float" -> Set("3.41"),
      "objects/int" -> Set("3"),
      "objects/boolean" -> Set("true"),
      "objects/nestedObject/nestedValue" -> Set("nested"),
      "objects/value" -> Set("val", "val2"),
      "objects/objectArray/v" -> Set("2"),
      "values" -> Set("arr1", "arr2")
    )
  }

  it should "ignore nulls for objects on base path" in {
    val source: DataSource = jsonSource(jsonWithNullObject)

    val entities = source.retrieve(EntitySchema("objects", typedPaths = IndexedSeq(UntypedPath.parse("value").asStringTypedPath))).entities
    entities.map(_.values).toSeq mustBe Seq(Seq(Seq("val")), Seq(Seq("val2")))
  }

  it should "retrieve path values with base path and type URI set" in {
    val source: DataSource = jsonSource(jsonComplex, basePath = "objects")
    val entities = source.retrieve(EntitySchema("nestedObject", typedPaths = IndexedSeq(UntypedPath.parse("nestedValue").asStringTypedPath))).entities
    entities.map(_.values).toSeq mustBe Seq(Seq(Seq("nested")))
  }

  it should "handle entity schema with sub paths and type URI" in {
    val source: DataSource = jsonSource(jsonWithNullObject)
    val entities = source.retrieve(EntitySchema("objects", typedPaths = IndexedSeq(UntypedPath.parse("nestedValue").asStringTypedPath), subPath = UntypedPath("nestedObject"))).entities
    entities.map(_.values).toSeq mustBe Seq(Seq(Seq("nested")))
  }

  it should "handle nested arrays" in {
    val source: DataSource = createSource(resources.get("example3.json"), "", "#id")
    val entities = source.retrieve(EntitySchema("person", typedPaths = IndexedSeq(UntypedPath.parse("name").asStringTypedPath))).entities
    entities.map(_.values).toSeq mustBe Seq(Seq(Seq("Peter")), Seq(Seq("John")))
  }

  it should "support * and #propertyName special paths" in {
    val source: DataSource = createSource(resources.get("exampleObjectKeys.json"), "", "#id")
    val paths = IndexedSeq("datum", "#propertyName", "*/#propertyName")
    val entities = source.retrieve(EntitySchema("SN/*", typedPaths = paths.map(UntypedPath.parse(_).asStringTypedPath))).entities
    entities.take(3).map(_.values).toSeq mustBe Seq(
      Seq(Seq("2022-01-01"), Seq("Neujahrstag"), Seq("datum", "hinweis")),
      Seq(Seq("2022-04-15"), Seq("Karfreitag"), Seq("datum", "hinweis")),
      Seq(Seq("2022-04-18"), Seq("Ostermontag"), Seq("datum", "hinweis"))
    )
  }

  class TestAnalyzer extends ValueAnalyzer[String] {
    private var maxString: Option[String] = None

    override def result: Option[String] = maxString

    override def update(value: String): Unit = maxString match {
      case Some(current) if current > value =>
        // Do nothing
      case _ =>
        maxString = Some(value)
    }
  }

  class TestAnalyzerFactory extends ValueAnalyzerFactory[String] {
    override def analyzer(): ValueAnalyzer[String] = new TestAnalyzer()
  }

  it should "extract schema" in {
    val schema = jsonExampleSource.extractSchema(new TestAnalyzerFactory(), Int.MaxValue, sampleLimit = None)
    schema.classes.size mustBe 4
    val classes = schema.classes
    classes.head mustBe ExtractedSchemaClass("", Seq())
    classes(1) mustBe ExtractedSchemaClass("persons", Seq(ExtractedSchemaProperty(path("id"),Some("1")), ExtractedSchemaProperty(path("name"),Some("Max"))))
    classes(2) mustBe ExtractedSchemaClass("persons/phoneNumbers", Seq(ExtractedSchemaProperty(path("type"),Some("office")), ExtractedSchemaProperty(path("number"),Some("789"))))
    classes(3) mustBe ExtractedSchemaClass("organizations", Seq(ExtractedSchemaProperty(path("name"),Some("John Inc"))))
  }

  it should "extract schema with value sample limit" in {
    val schema = jsonExampleSource.extractSchema(new TestAnalyzerFactory(), Int.MaxValue, sampleLimit = Some(1))
    schema.classes.size mustBe 4
    val classes = schema.classes
    classes.head mustBe ExtractedSchemaClass("", Seq())
    classes(1) mustBe ExtractedSchemaClass("persons", Seq(ExtractedSchemaProperty(path("id"),Some("0")), ExtractedSchemaProperty(path("name"),Some("John"))))
    classes(2) mustBe ExtractedSchemaClass("persons/phoneNumbers", Seq(ExtractedSchemaProperty(path("type"),Some("home")), ExtractedSchemaProperty(path("number"),Some("123"))))
    classes(3) mustBe ExtractedSchemaClass("organizations", Seq(ExtractedSchemaProperty(path("name"),Some("John Inc"))))
  }

  it should "extract schema with base path set" in {
    for(basePath <- Seq("persons", "persons")) {
      val source = createSource(resources.get("example.json"), basePath, "#id")
      val schema = source.extractSchema(new TestAnalyzerFactory(), Int.MaxValue, sampleLimit = None)
      schema.classes.size mustBe 2
      val classes = schema.classes
      classes.head mustBe ExtractedSchemaClass("", Seq(ExtractedSchemaProperty(path("id"),Some("1")), ExtractedSchemaProperty(path("name"),Some("Max"))))
      classes(1) mustBe ExtractedSchemaClass("phoneNumbers",
        Seq(ExtractedSchemaProperty(path("type"),Some("office")), ExtractedSchemaProperty(path("number"),Some("789"))))
    }
  }

  it should "return typed paths" in {
    val typedPaths = jsonExampleSource.retrievePaths("")
    typedPaths.map(tp => (tp.toUntypedPath.normalizedSerialization, tp.valueType)) mustBe IndexedSeq(
      "persons" -> ValueType.URI,
      "persons/id" -> ValueType.STRING ,
      "persons/name" -> ValueType.STRING,
      "persons/phoneNumbers" -> ValueType.URI,
      "persons/phoneNumbers/type" -> ValueType.STRING,
      "persons/phoneNumbers/number" -> ValueType.STRING,
      "organizations" -> ValueType.URI,
      "organizations/name" -> ValueType.STRING
    )
  }

  it should "work with json files that use spaces in keys" in {
    val source2 = createSource(resources.get("example2.json"), "", "#id")
    source2.retrieveTypes().map(_._1).toSet mustBe Set("", "values+with+spaces")
    source2.retrievePaths("values+with+spaces").map(_.toUntypedPath.normalizedSerialization) mustBe IndexedSeq("space+value")

    val entities = source2.retrieve(EntitySchema("values+with+spaces", typedPaths = IndexedSeq(UntypedPath.parse("space+value").asStringTypedPath))).entities
    entities.map(_.values).toSeq mustBe Seq(Seq(Seq("Berlin")), Seq(Seq("Hamburg")))
  }

  it should "generate consistent URIs for array values" in {
    val source2 = createSource(resources.get("exampleArrays.json"), "", "")

    val entities1 = source2.retrieve(EntitySchema("", typedPaths = IndexedSeq(UntypedPath.parse("data").asStringTypedPath))).entities.toList
    val entities2 = source2.retrieve(EntitySchema("", typedPaths = IndexedSeq(UntypedPath.parse("data").asUriTypedPath))).entities.toList
    val entities3 = source2.retrieve(EntitySchema("data", typedPaths = IndexedSeq())).entities.toList

    entities1.head.values.head mustBe Seq("A", "B")
    entities2.head.values.head mustBe entities3.map(_.uri.uri)
  }

  it should "not generate any entities for this object path" in {
    val source = createSource(resources.get("objectPathTest.json"), "", "")
    val entities = source.retrieve(EntitySchema("pathA/pathA", typedPaths = IndexedSeq(UntypedPath("#text").asStringTypedPath))).entities.toList
    entities mustBe empty
  }

  it should "work with bulk zip files" in {
    val jsonDataset = createDataset(ReadOnlyResource(resources.get("example.zip")))
    val source = jsonDataset.source
    val paths = source.retrievePaths("persons")
    // Check for merged schema
    paths.map(_.normalizedSerialization) mustBe IndexedSeq("id", "name", "phoneNumbers", "additionalProperty")
    val entities = source.retrieve(EntitySchema("persons", typedPaths = IndexedSeq(UntypedPath("id").asStringTypedPath))).entities.toList
    entities must have size 3
  }

  it should "for JSON Lines documents: collect all paths" in {
    val paths = jsonLinesSource.collectPaths(Int.MaxValue)
    paths.map(_.mkString("/")) mustBe
      Seq("", "id", "name", "phoneNumbers", "phoneNumbers/type", "phoneNumbers/number")
  }

  it should "for JSON Lines documents: retrieve values" in {
    val schema = new EntitySchema(
      typeUri = Uri(""),
      typedPaths = IndexedSeq(
        UntypedPath.parse("/name").asStringTypedPath,
        UntypedPath.parse("/phoneNumbers/number").asStringTypedPath)
    )
    val result = jsonLinesSource.retrieve(schema).entities.toSeq
    result.size mustBe 2
    result(0).values mustBe IndexedSeq(Seq("John"), Seq("123", "456"))
    result(1).values mustBe IndexedSeq(Seq("Max"), Seq("789"))
  }

  private def jsonSource(json: String, basePath: String = ""): JsonSource = {
    val jsonResource = InMemoryResourceManager().get("temp.json")
    jsonResource.writeString(json)
    createSource(jsonResource, basePath = basePath, uriPattern = "")
  }

  private def path(str: String): TypedPath = {
    UntypedPath(str).asStringTypedPath
  }
}
