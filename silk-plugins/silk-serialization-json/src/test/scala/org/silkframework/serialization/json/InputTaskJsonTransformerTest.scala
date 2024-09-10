package org.silkframework.serialization.json

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.dataset.DatasetSpec
import org.silkframework.plugins.dataset.csv.CsvDataset
import org.silkframework.rule.TaskContext
import org.silkframework.runtime.activity.TestUserContextTrait
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.resource.WritableResource
import org.silkframework.runtime.serialization.WriteContext
import org.silkframework.serialization.json.JsonSerializers._
import org.silkframework.workspace.{Project, ProjectTask, TestWorkspaceProviderTestTrait}
import play.api.libs.json.JsValue

class InputTaskJsonTransformerTest extends AnyFlatSpec with Matchers with TestWorkspaceProviderTestTrait with TestUserContextTrait {

  behavior of "TaskProperty"

  it should "retrieve the full JSON of the input dataset" in {
    implicit val writeContext: WriteContext[JsValue] = WriteContext.fromProject(TestData.project)
    val expectedJson = JsonSerialization.toJson[Task[TaskSpec]](TestData.dataset)
    TestData.retrieve("") shouldBe expectedJson.toString()
  }

  it should "retrieve a single property of the input dataset" in {
    TestData.retrieve("data/parameters/file") shouldBe TestData.resource.name
  }


  override def workspaceProviderId: String = "inMemory"

  object TestData {

    val project: Project = retrieveOrCreateProject("testProject")
    val resource: WritableResource = project.resources.get("test")
    resource.writeString("ABC")
    val dataset: ProjectTask[DatasetSpec[CsvDataset]] = project.addTask("input", DatasetSpec(CsvDataset(file = resource)))

    def retrieve(path: String): String = {
      val taskContext = TaskContext(Seq(dataset), PluginContext.fromProject(project))
      val transformer = InputTaskJsonTransformer(path).withContext(taskContext)
      transformer(Seq.empty).head
    }

  }

}
