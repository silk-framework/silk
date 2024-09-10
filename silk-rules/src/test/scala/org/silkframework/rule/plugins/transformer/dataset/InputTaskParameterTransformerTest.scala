package org.silkframework.rule.plugins.transformer.dataset

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.config.{PlainTask, Prefixes}
import org.silkframework.dataset.DatasetSpec
import org.silkframework.plugins.dataset.csv.CsvDataset
import org.silkframework.rule.TaskContext
import org.silkframework.runtime.activity.TestUserContextTrait
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.resource.InMemoryResourceManager

class InputTaskParameterTransformerTest extends AnyFlatSpec with Matchers with TestUserContextTrait {

  behavior of "InputTaskParameterTransformer"

  it should "retrieve parameter values" in {
    val resources = InMemoryResourceManager()
    val resource = resources.get("test")
    resource.writeString("ABC")
    val dataset = PlainTask("input", DatasetSpec(CsvDataset(file = resource)))
    implicit val taskContext: TaskContext = TaskContext(Seq(dataset), PluginContext(Prefixes.empty, resources))

    value("separator") shouldBe ","
  }

  private def value(parameter: String)(implicit taskContext: TaskContext): String = {
    val transformer = InputTaskParameterTransformer(parameter).withContext(taskContext)
    transformer(Seq.empty).head
  }


}

