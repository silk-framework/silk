package org.silkframework.serialization.json.transformer

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.dataset.DatasetSpec
import org.silkframework.plugins.dataset.csv.CsvDataset
import org.silkframework.rule.TaskContext
import org.silkframework.runtime.activity.TestUserContextTrait
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.resource.WritableResource
import org.silkframework.workspace.{Project, ProjectTask, TestWorkspaceProviderTestTrait}
import play.api.libs.json.{JsDefined, JsString, Json}

class InputFileAttributesTransformerTest extends AnyFlatSpec with Matchers with TestWorkspaceProviderTestTrait with TestUserContextTrait {

  behavior of "InputFileAttributesTransformer"

  it should "retrieve the full JSON of the input file" in {
    val json = Json.parse(TestData.retrieve(""))
    json \ "name" shouldBe JsDefined(JsString(TestData.resource.name))
  }

  it should "retrieve a single property of the input dataset" in {
    TestData.retrieve("name") shouldBe TestData.resource.name
  }


  override def workspaceProviderId: String = "inMemory"

  object TestData {

    val project: Project = retrieveOrCreateProject("testProject")
    val resource: WritableResource = project.resources.get("test")
    resource.writeString("ABC")
    val dataset: ProjectTask[DatasetSpec[CsvDataset]] = project.addTask("input", DatasetSpec(CsvDataset(file = resource)))

    def retrieve(path: String): String = {
      val taskContext = TaskContext(Seq(dataset), PluginContext.fromProject(project))
      val transformer = InputFileAttributesTransformer(path).withContext(taskContext)
      transformer(Seq.empty).head
    }

  }

}
