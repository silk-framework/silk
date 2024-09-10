package org.silkframework.rule.plugins.transformer.dataset

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.config.PlainTask
import org.silkframework.dataset.DatasetSpec
import org.silkframework.plugins.dataset.csv.CsvDataset
import org.silkframework.rule.TaskContext
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.resource.InMemoryResourceManager

class FileHashTransformerTest extends AnyFlatSpec with Matchers {

  behavior of "FileHashTransformer"

  it should "generate the hash sum of the specified file" in {
    val resource = InMemoryResourceManager().get("test")
    resource.writeString("ABC")
    val transformer = FileHashTransformer(Some(resource))
    transformer(Seq.empty) shouldBe Seq("b5d4045c3f466fa91fe2cc6abe79232a1a57cdf104f7a26e716e0a1e2789df78")
  }

  it should "generate the hash sum of the input dataset" in {
    val resource = InMemoryResourceManager().get("test")
    resource.writeString("ABC")
    val dataset = PlainTask("input", DatasetSpec(CsvDataset(file = resource)))
    val taskContext = TaskContext(Seq(dataset), PluginContext.empty)

    val transformer = FileHashTransformer(None).withContext(taskContext)
    transformer(Seq.empty) shouldBe Seq("b5d4045c3f466fa91fe2cc6abe79232a1a57cdf104f7a26e716e0a1e2789df78")
  }

}
