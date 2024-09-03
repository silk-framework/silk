package org.silkframework.rule.plugins.transformer.dataset

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.config.PlainTask
import org.silkframework.dataset.DatasetSpec
import org.silkframework.plugins.dataset.csv.CsvDataset
import org.silkframework.rule.TaskContext
import org.silkframework.runtime.resource.InMemoryResourceManager

class FileHashTransformerTest extends AnyFlatSpec with Matchers {

  behavior of "FileHashTransformer"

  it should "generate the hash sum of the specified file" in {
    val resource = InMemoryResourceManager().get("test")
    resource.writeString("ABC")
    val transformer = FileHashTransformer(Some(resource))
    transformer(Seq.empty) shouldBe Seq("3c01bdbb26f358bab27f267924aa2c9a03fcfdb8")
  }

  it should "generate the hash sum of the input dataset" in {
    val resource = InMemoryResourceManager().get("test")
    resource.writeString("ABC")
    val dataset = PlainTask("input", DatasetSpec(CsvDataset(file = resource)))
    val taskContext = TaskContext(Seq(dataset))

    val transformer = FileHashTransformer(None).withContext(taskContext)
    transformer(Seq.empty) shouldBe Seq("3c01bdbb26f358bab27f267924aa2c9a03fcfdb8")
  }

}
