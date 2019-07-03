package org.silkframework.plugins.dataset.json

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.dataset._
import org.silkframework.entity._
import org.silkframework.entity.paths.{TypedPath, UntypedPath}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.{ClasspathResourceLoader, InMemoryResourceManager}
import org.silkframework.util.Uri

import scala.collection.mutable
import scala.io.Codec

class JsonSourceTest extends FlatSpec with MustMatchers {
  behavior of "Json Source"

  implicit val userContext: UserContext = UserContext.Empty

  private val resources = ClasspathResourceLoader("org/silkframework/plugins/dataset/json/")

  private def jsonExampleSource: JsonSource = {
    val source = JsonSource(resources.get("example.json"), "", "#id", Codec.UTF8)
    source
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
    val source = JsonSource(resource, "data", "http://blah", Codec.UTF8)
    val entities = source.retrieve(EntitySchema.empty)
    entities mustBe empty
  }

  it should "not return entities for valid paths" in {
    val resourceManager = InMemoryResourceManager()
    val resource = resourceManager.get("test.json")
    resource.writeString(
      """
        |{"data":{"entities":[{"id":"ID"}]}}
      """.stripMargin)
    val source = JsonSource(resource, "data/entities", "http://blah/{id}", Codec.UTF8)
    val entities = source.retrieve(EntitySchema.empty)
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
    result.size mustBe 3
    result.map(_.values) mustBe Seq(IndexedSeq(Seq("123")), IndexedSeq(Seq("456")), IndexedSeq(Seq("789")))
  }

  it should "return all paths including intermediate paths for retrieve paths" in {
    val paths = jsonExampleSource.retrievePaths(Uri(""), depth = Int.MaxValue)
    paths.size mustBe 8
    paths must contain allOf(TypedPath("/persons", UriValueType), TypedPath("/persons/phoneNumbers", UriValueType))
  }

  it should "return all paths of depth 1" in {
    val paths = jsonExampleSource.retrievePaths(Uri(""), depth = 1)
    paths.map(_.toSimplePath.normalizedSerialization) mustBe Seq("persons", "organizations")
  }

  it should "return all paths of depth 2" in {
    val paths = jsonExampleSource.retrievePaths(Uri(""), depth = 2)
    paths.map(_.toSimplePath.normalizedSerialization) mustBe Seq("persons", "persons/id", "persons/name", "persons/phoneNumbers", "organizations", "organizations/name")
  }

  it should "return all paths of depth 1 of sub path" in {
    val paths = jsonExampleSource.retrievePaths(Uri("/persons"), depth = 1)
    paths.map(_.toSimplePath.normalizedSerialization) mustBe Seq("id", "name", "phoneNumbers")
  }

  it should "return all paths of depth 2 of sub path" in {
    val paths = jsonExampleSource.retrievePaths(Uri("/persons"), depth = 2)
    paths.map(_.toSimplePath.normalizedSerialization) mustBe Seq("id", "name", "phoneNumbers", "phoneNumbers/type", "phoneNumbers/number")
  }

  it should "return all paths of depth 1 of sub path of length 2" in {
    val paths = jsonExampleSource.retrievePaths(Uri("/persons/phoneNumbers"), depth = 1)
    paths.map(_.toSimplePath.normalizedSerialization) mustBe Seq("type", "number")
  }

  it should "list all leaf paths of the root" in {
    val paths = jsonExampleSource.retrieveJsonPaths(Uri(""), depth = Int.MaxValue, limit = None, leafPathsOnly = true, innerPathsOnly = false)
    paths.map(_._1.normalizedSerialization) mustBe Seq("persons/id", "persons/name", "persons/phoneNumbers/type", "persons/phoneNumbers/number", "organizations/name")
  }

  it should "list all leaf paths of a sub path" in {
    val paths = jsonExampleSource.retrieveJsonPaths(Uri("persons"), depth = Int.MaxValue, limit = None, leafPathsOnly = true, innerPathsOnly = false)
    paths.map(_._1.normalizedSerialization) mustBe Seq("id", "name", "phoneNumbers/type", "phoneNumbers/number")
  }

  it should "list all leaf paths of depth 1 of a sub path" in {
    val paths = jsonExampleSource.retrieveJsonPaths(Uri("persons"), depth = 1, limit = None, leafPathsOnly = true, innerPathsOnly = false)
    paths.map(_._1.normalizedSerialization) mustBe Seq("id", "name")
  }

  it should "return valid URIs for resource paths" in {
    val result = jsonExampleSource.retrieve(EntitySchema(Uri(""), typedPaths = IndexedSeq(UntypedPath.parse("/persons").asStringTypedPath)))
    val uris = result.flatMap(_.values.flatten).toSeq
    for(uri <- uris) {
      assert(Uri(uri).isValidUri, s"URI $uri was not valid!")
    }
    uris.distinct.size mustBe uris.size
  }

  private val jsonWithNull = """{"values": ["val", null]}"""

  it should "return JSON null values as missing values" in {
    val source: DataSource = jsonSource(jsonWithNull)
    val entities = source.retrieve(EntitySchema("", typedPaths = IndexedSeq(UntypedPath("values").asStringTypedPath)))
    entities.map(_.values) mustBe Seq(Seq(Seq("val")))
  }

  private val jsonWithNullObject =
    """{"objects": [
      |  {"value":"val", "nestedObject": {"nestedValue": "nested"}},
      |  null,
      |  {"value":"val2"}
      |]}""".stripMargin

  it should "be able to ignore null JSON objects in the middle of longer paths" in {
    val source: DataSource = jsonSource(jsonWithNullObject)

    val entities = source.retrieve(EntitySchema("", typedPaths = IndexedSeq(UntypedPath.parse("objects/value").asStringTypedPath)))
    entities.map(_.values) mustBe Seq(Seq(Seq("val", "val2")))
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

    val entities = source.retrieve(EntitySchema("objects", typedPaths = IndexedSeq(UntypedPath.parse("value").asStringTypedPath)))
    entities.map(_.values) mustBe Seq(Seq(Seq("val")), Seq(Seq("val2")))
  }

  it should "handle entity schema with sub paths and type URI" in {
    val source: DataSource = jsonSource(jsonWithNullObject)
    val entities = source.retrieve(EntitySchema("objects", typedPaths = IndexedSeq(UntypedPath.parse("nestedValue").asStringTypedPath), subPath = UntypedPath("nestedObject")))
    entities.map(_.values) mustBe Seq(Seq(Seq("nested")))
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
    classes(1) mustBe ExtractedSchemaClass("persons", Seq(ExtractedSchemaProperty(UntypedPath("id"),Some("1")), ExtractedSchemaProperty(UntypedPath("name"),Some("Max"))))
    classes(2) mustBe ExtractedSchemaClass("persons/phoneNumbers", Seq(ExtractedSchemaProperty(UntypedPath("type"),Some("office")), ExtractedSchemaProperty(UntypedPath("number"),Some("789"))))
    classes(3) mustBe ExtractedSchemaClass("organizations", Seq(ExtractedSchemaProperty(UntypedPath("name"),Some("John Inc"))))
  }

  it should "extract schema with value sample limit" in {
    val schema = jsonExampleSource.extractSchema(new TestAnalyzerFactory(), Int.MaxValue, sampleLimit = Some(1))
    schema.classes.size mustBe 4
    val classes = schema.classes
    classes.head mustBe ExtractedSchemaClass("", Seq())
    classes(1) mustBe ExtractedSchemaClass("persons", Seq(ExtractedSchemaProperty(UntypedPath("id"),Some("0")), ExtractedSchemaProperty(UntypedPath("name"),Some("John"))))
    classes(2) mustBe ExtractedSchemaClass("persons/phoneNumbers", Seq(ExtractedSchemaProperty(UntypedPath("type"),Some("home")), ExtractedSchemaProperty(UntypedPath("number"),Some("123"))))
    classes(3) mustBe ExtractedSchemaClass("organizations", Seq(ExtractedSchemaProperty(UntypedPath("name"),Some("John Inc"))))
  }

  it should "extract schema with base path set" in {
    for(basePath <- Seq("persons", "persons")) {
      val schema = jsonExampleSource.copy(basePath = basePath).extractSchema(new TestAnalyzerFactory(), Int.MaxValue, sampleLimit = None)
      schema.classes.size mustBe 2
      val classes = schema.classes
      classes.head mustBe ExtractedSchemaClass("", Seq(ExtractedSchemaProperty(UntypedPath("id"),Some("1")), ExtractedSchemaProperty(UntypedPath("name"),Some("Max"))))
      classes(1) mustBe ExtractedSchemaClass("phoneNumbers",
        Seq(ExtractedSchemaProperty(UntypedPath("type"),Some("office")), ExtractedSchemaProperty(UntypedPath("number"),Some("789"))))
    }
  }

  it should "return typed paths" in {
    val typedPaths = jsonExampleSource.retrievePaths("")
    typedPaths.map(tp => (tp.toSimplePath.normalizedSerialization, tp.valueType)) mustBe IndexedSeq(
      "persons" -> UriValueType,
      "persons/id" -> StringValueType,
      "persons/name" -> StringValueType,
      "persons/phoneNumbers" -> UriValueType,
      "persons/phoneNumbers/type" -> StringValueType,
      "persons/phoneNumbers/number" -> StringValueType,
      "organizations" -> UriValueType,
      "organizations/name" -> StringValueType
    )
  }

  it should "test string based apply method" in {
    val str = resources.get("example.json").loadAsString(Codec.UTF8)
    val result = JsonSource(str, "", "#id").peak(EntitySchema(Uri(""), typedPaths = IndexedSeq(UntypedPath.parse("/persons/phoneNumbers/number").asStringTypedPath)), 3).toSeq
    result.size mustBe 1
    result.head.values mustBe IndexedSeq(Seq("123", "456", "789"))
  }

  private def jsonSource(json: String): JsonSource = {
    val jsonResource = InMemoryResourceManager().get("temp.json")
    jsonResource.writeString(json)
    val source = JsonDataset(jsonResource).source
    source.asInstanceOf[JsonSource]
  }
}
