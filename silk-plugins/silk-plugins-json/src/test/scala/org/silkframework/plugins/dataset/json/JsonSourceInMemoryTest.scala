package org.silkframework.plugins.dataset.json
import org.silkframework.dataset.DataSource
import org.silkframework.entity.EntitySchema
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.runtime.resource.{Resource, WritableResource}
import org.silkframework.util.Uri

import scala.io.Codec

class JsonSourceInMemoryTest extends JsonSourceTest {

  behavior of "JsonSourceInMemory"

  override protected def createSource(resource: Resource, basePath: String, uriPattern: String): JsonSource = {
    JsonSourceInMemory.fromResource(resource, basePath, uriPattern)
  }

  it should "support ** special path" in {
    val source1: DataSource = createSource(resources.get("exampleDifferentNesting.json"), "", "#id")

    val entities1 = source1.retrieve(EntitySchema("**/person", typedPaths = IndexedSeq(UntypedPath.parse("name").asStringTypedPath))).entities
    entities1.map(_.values.head).toSeq mustBe Seq(Seq("John"), Seq("Alice"))

    val entities2 = source1.retrieve(EntitySchema("", typedPaths = IndexedSeq(UntypedPath.parse("**/name").asStringTypedPath))).entities
    entities2.map(_.values.head).toSeq mustBe Seq(Seq("John", "Alice"))

    val entities3 = source1.retrieve(EntitySchema("**", typedPaths = IndexedSeq(UntypedPath.parse("name").asStringTypedPath))).entities
    entities3.map(_.values.head).filterNot(_.isEmpty).toSeq mustBe Seq(Seq("John"), Seq("Alice"))

    val source2: DataSource = createSource(resources.get("exampleDifferentNesting2.json"), "", "#id")

    val entities4 = source2.retrieve(EntitySchema("**", typedPaths = IndexedSeq(UntypedPath.parse("userId").asStringTypedPath))).entities
    entities4.map(_.values.head).toSeq mustBe Seq(Seq("1"), Seq("2"), Seq("3"))
  }

  it should "not allow multiple ** operators in a single path" in {
    val source: DataSource = createSource(resources.get("exampleDifferentNesting.json"), "", "#id")

    an[IllegalArgumentException] should be thrownBy {
      source.retrieve(EntitySchema("**/person/**", typedPaths = IndexedSeq(UntypedPath.parse("name").asStringTypedPath))).entities.toSeq
    }

    an[IllegalArgumentException] should be thrownBy {
      source.retrieve(EntitySchema("", typedPaths = IndexedSeq(UntypedPath.parse("**/name/**").asStringTypedPath))).entities.toSeq
    }
  }

  it should "test string based apply method" in {
    val str = resources.get("example.json").loadAsString(Codec.UTF8)
    val result = JsonSourceInMemory.fromString("taskId", str, "", "#id").peak(EntitySchema(Uri(""), typedPaths = IndexedSeq(UntypedPath.parse("/persons/phoneNumbers/number").asStringTypedPath)), 3).toSeq
    result.size mustBe 1
    result.head.values mustBe IndexedSeq(Seq("123", "456", "789"))
  }

  override protected def createDataset(resource: WritableResource): JsonDataset = {
    JsonDataset(resource, streaming = false)
  }
}
