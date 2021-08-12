package org.silkframework.plugins.dataset.json
import org.silkframework.entity.EntitySchema
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.runtime.resource.Resource
import org.silkframework.util.Uri

import scala.io.Codec

class JsonSourceInMemoryTest extends JsonSourceTest {

  behavior of "JsonSourceInMemory"

  override protected def createSource(resource: Resource, basePath: String, uriPattern: String): JsonSource = {
    JsonSourceInMemory(resource, basePath, uriPattern)
  }

  it should "test string based apply method" in {
    val str = resources.get("example.json").loadAsString(Codec.UTF8)
    val result = JsonSourceInMemory("taskId", str, "", "#id").peak(EntitySchema(Uri(""), typedPaths = IndexedSeq(UntypedPath.parse("/persons/phoneNumbers/number").asStringTypedPath)), 3).toSeq
    result.size mustBe 1
    result.head.values mustBe IndexedSeq(Seq("123", "456", "789"))
  }

  it should "list all leaf paths of the root" in {
    val paths = jsonExampleSource.asInstanceOf[JsonSourceInMemory].retrieveJsonPaths(Uri(""), depth = Int.MaxValue, limit = None, leafPathsOnly = true, innerPathsOnly = false)
    paths.map(_._1.normalizedSerialization) mustBe Seq("persons/id", "persons/name", "persons/phoneNumbers/type", "persons/phoneNumbers/number", "organizations/name")
  }

  it should "list all leaf paths of a sub path" in {
    val paths = jsonExampleSource.asInstanceOf[JsonSourceInMemory].retrieveJsonPaths(Uri("persons"), depth = Int.MaxValue, limit = None, leafPathsOnly = true, innerPathsOnly = false)
    paths.map(_._1.normalizedSerialization) mustBe Seq("id", "name", "phoneNumbers/type", "phoneNumbers/number")
  }

  it should "list all leaf paths of depth 1 of a sub path" in {
    val paths = jsonExampleSource.asInstanceOf[JsonSourceInMemory].retrieveJsonPaths(Uri("persons"), depth = 1, limit = None, leafPathsOnly = true, innerPathsOnly = false)
    paths.map(_._1.normalizedSerialization) mustBe Seq("id", "name")
  }
}
