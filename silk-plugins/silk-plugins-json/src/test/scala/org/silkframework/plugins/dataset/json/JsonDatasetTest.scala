package org.silkframework.plugins.dataset.json


import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.config.Prefixes
import org.silkframework.entity.EntitySchema
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.runtime.activity.TestUserContextTrait
import org.silkframework.runtime.resource.{ClasspathResourceLoader, Resource, ResourceLoader, ResourceTooLargeException}
import org.silkframework.util.{ConfigTestTrait, Uri}

class JsonDatasetTest extends AnyFlatSpec with Matchers with TestUserContextTrait {

  behavior of "JSON dataset"

  protected val resources: ResourceLoader = ClasspathResourceLoader("org/silkframework/plugins/dataset/json/")

  it should "not load large files into memory" in {
    // Each line should be loaded separately, so this should work even though the overall file is larger than the limit
    noException should be thrownBy loadEntities(maxInMemorySize = "130B")
    // If a line is too large, reading should be stopped
    an[ResourceTooLargeException] should be thrownBy loadEntities(maxInMemorySize = "100B")
  }

  private def loadEntities(maxInMemorySize: String): Unit = {
    ConfigTestTrait.withConfig(Resource.maxInMemorySizeParameterName -> Some(maxInMemorySize)) {
      implicit val prefixes: Prefixes = Prefixes.empty
      val source = JsonSourceInMemory(resources.get("exampleLines.jsonl"), "", "")
      source.retrieve(EntitySchema(Uri(""), typedPaths = IndexedSeq(UntypedPath.parse("name").asStringTypedPath))).entities.toSeq
    }
  }
}
