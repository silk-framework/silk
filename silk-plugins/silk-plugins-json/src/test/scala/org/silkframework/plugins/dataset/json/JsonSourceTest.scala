package org.silkframework.plugins.dataset.json

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.dataset.DataSource
import org.silkframework.entity.{EntitySchema, Path}
import org.silkframework.runtime.resource.{ClasspathResourceLoader, InMemoryResourceManager}
import org.silkframework.util.Uri

import scala.io.Codec

class JsonSourceTest extends FlatSpec with MustMatchers {
  behavior of "Json Source"

  private def jsonExampleSource: JsonSource = {
    val resources = ClasspathResourceLoader("org/silkframework/plugins/dataset/json/")
    val source = new JsonSource(resources.get("example.json"), "", "#id", Codec.UTF8)
    source
  }

  it should "return all inner node types" in {
    val types = jsonExampleSource.retrieveTypes().map(_._1).toSet
    types mustBe Set(
      "",
      "/persons",
      "/persons/phoneNumbers",
      "/organizations"
    )
  }

  it should "not return an entity for an empty JSON array" in {
    val resourceManager = InMemoryResourceManager()
    val resource = resourceManager.get("test.json")
    resource.writeString(
      """
        |{"data":[]}
      """.stripMargin)
    val source = new JsonSource(resource, "data", "http://blah", Codec.UTF8)
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
    val source = new JsonSource(resource, "data/entities", "http://blah/{id}", Codec.UTF8)
    val entities = source.retrieve(EntitySchema.empty)
    entities.size mustBe 1
    entities.head.uri mustBe "http://blah/ID"
  }

  it should "return peak results" in {
    val result = jsonExampleSource.peak(EntitySchema(Uri(""), typedPaths = IndexedSeq(Path.parse("/persons/phoneNumbers/number").asStringTypedPath)), 3).toSeq
    result.size mustBe 1
    result.head.values mustBe IndexedSeq(Seq("123", "456", "789"))
  }

  it should "return peak results with sub path set" in {
    val result = jsonExampleSource.peak(EntitySchema(Uri(""), typedPaths = IndexedSeq(Path.parse("/number").asStringTypedPath),
      subPath = Path.parse("/persons/phoneNumbers")), 3).toSeq
    result.size mustBe 3
    result.map(_.values) mustBe Seq(IndexedSeq(Seq("123")), IndexedSeq(Seq("456")), IndexedSeq(Seq("789")))
  }

  it should "return all paths including intermediate paths for retrieve paths" in {
    val paths = jsonExampleSource.retrievePaths(Uri(""))
    paths.size mustBe 9
    paths must contain allOf(Path.parse("/persons"), Path.parse("/persons/phoneNumbers"))
  }

  it should "return valid URIs for resource paths" in {
    val result = jsonExampleSource.retrieve(EntitySchema(Uri(""), typedPaths = IndexedSeq(Path.parse("/persons").asStringTypedPath)))
    val uris = result.flatMap(_.values.flatten).toSeq
    for(uri <- uris) {
      assert(Uri(uri).isValidUri, s"URI $uri was not valid!")
    }
    uris.distinct.size mustBe uris.size
  }

  private val jsonWithNull = """{"values": ["val", null]}"""

  it should "return JSON null values as missing values" in {
    val source: DataSource = jsonSource(jsonWithNull)
    val entities = source.retrieve(EntitySchema("", typedPaths = IndexedSeq(Path("values").asStringTypedPath)))
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

    val entities = source.retrieve(EntitySchema("", typedPaths = IndexedSeq(Path.parse("objects/value").asStringTypedPath)))
    entities.map(_.values) mustBe Seq(Seq(Seq("val", "val2")))
  }

  it should "ignore nulls for objects on base path" in {
    val source: DataSource = jsonSource(jsonWithNullObject)

    val entities = source.retrieve(EntitySchema("objects", typedPaths = IndexedSeq(Path.parse("value").asStringTypedPath)))
    entities.map(_.values) mustBe Seq(Seq(Seq("val")), Seq(Seq("val2")))
  }

  it should "handle entity schema with sub paths and type URI" in {
    val source: DataSource = jsonSource(jsonWithNullObject)
    val entities = source.retrieve(EntitySchema("nestedObject", typedPaths = IndexedSeq(Path.parse("nestedValue").asStringTypedPath), subPath = Path("objects")))
    entities.map(_.values) mustBe Seq(Seq(Seq("nested")))
  }

  private def jsonSource(json: String): JsonSource = {
    val jsonResource = InMemoryResourceManager().get("temp.json")
    jsonResource.writeString(json)
    val source = JsonDataset(jsonResource).source
    source.asInstanceOf[JsonSource]
  }
}
