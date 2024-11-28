package org.silkframework.rule.plugins.transformer.metadata

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.config.{PlainTask, Prefixes}
import org.silkframework.dataset.DatasetSpec
import org.silkframework.plugins.dataset.csv.CsvDataset
import org.silkframework.rule.TaskContext
import org.silkframework.runtime.activity.{TestUserContextTrait, UserContext}
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.resource.{InMemoryResourceManager, WritableResource}
import play.api.libs.json.{JsDefined, JsString, Json}

class InputFileAttributesTransformerTest extends AnyFlatSpec with Matchers with TestUserContextTrait {

  behavior of "InputFileAttributesTransformer"

  it should "retrieve a single property of the input dataset" in {
    TestData.retrieve(FileAttributeEnum.name) shouldBe TestData.resource.name
  }

  object TestData {

    private val resources = InMemoryResourceManager()
    val resource: WritableResource = resources.get("test")
    resource.writeString("ABC")
    private val dataset = PlainTask("inputTask", DatasetSpec(CsvDataset(file = resource)))

    def retrieve(attribute: FileAttributeEnum): String = {
      val taskContext = TaskContext(Seq(dataset), PluginContext(Prefixes.empty, resources, UserContext.Empty))
      val transformer = InputFileAttributesTransformer(attribute).withContext(taskContext)
      transformer(Seq.empty).head
    }

  }

}
