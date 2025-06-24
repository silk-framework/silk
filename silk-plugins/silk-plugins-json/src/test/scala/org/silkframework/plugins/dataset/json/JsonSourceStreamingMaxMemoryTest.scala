package org.silkframework.plugins.dataset.json


import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.silkframework.entity.EntitySchema
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.runtime.activity.TestPluginContextTrait
import org.silkframework.runtime.resource.{ClasspathResource, ResourceTooLargeException}
import org.silkframework.util.{ConfigTestTrait, Uri}

class JsonSourceStreamingMaxMemoryTest extends AnyFlatSpec with Matchers with ConfigTestTrait with TestPluginContextTrait {

  behavior of "JsonSourceStreaming"

  private val memoryTestJson = ClasspathResource("org/silkframework/plugins/dataset/json/memoryTest.json")

  private val source = new JsonSourceStreaming("testSource", memoryTestJson, "", "")

  it should "not load large nodes into memory" in {
    noException should be thrownBy loadEntities("smallString")
    an[ResourceTooLargeException] should be thrownBy loadEntities("largeString")
    noException should be thrownBy loadEntities("smallEntity")
    an[ResourceTooLargeException] should be thrownBy loadEntities("largeEntity")
  }

  private def loadEntities(path: String): Unit = {
    val result = source.retrieve(EntitySchema(Uri(path), typedPaths = IndexedSeq(UntypedPath.parse("/value").asStringTypedPath))).entities
    result.toList
  }

  override def propertyMap: Map[String, Option[String]] = {
    Map(
      "org.silkframework.runtime.resource.Resource.maxInMemorySize" -> Some("48b")
    )
  }
}
