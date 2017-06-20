package org.silkframework.plugins.dataset.json

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.entity.EntitySchema
import org.silkframework.runtime.resource.InMemoryResourceManager

import scala.io.Codec

/**
  * Created on 12/22/16.
  */
class JsonSourceTest extends FlatSpec with MustMatchers {
  behavior of "Json Source"

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
}
