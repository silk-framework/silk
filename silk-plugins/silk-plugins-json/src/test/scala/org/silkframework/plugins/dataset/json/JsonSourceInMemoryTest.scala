package org.silkframework.plugins.dataset.json
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
