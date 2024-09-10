package org.silkframework.serialization.json

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.dataset.DatasetSpec
import org.silkframework.plugins.dataset.csv.CsvDataset
import org.silkframework.rule.TaskContext
import org.silkframework.runtime.activity.{TestUserContextTrait, UserContext}
import org.silkframework.runtime.serialization.WriteContext
import org.silkframework.serialization.json.JsonSerializers._
import org.silkframework.workspace.TestWorkspaceProviderTestTrait
import play.api.libs.json.JsValue

class TaskJsonTransformerTest extends AnyFlatSpec with Matchers with TestWorkspaceProviderTestTrait with TestUserContextTrait {

  behavior of "TaskProperty"

  it should "retrieve a property of the input dataset" in {
    val project = retrieveOrCreateProject("testProject")

    // Add test dataset
    val resource = project.resources.get("test")
    resource.writeString("ABC")
    val dataset = project.addTask("input", DatasetSpec(CsvDataset(file = resource)))

    // Execute transformer
    val taskContext = TaskContext(Seq(dataset), UserContext.Empty)
    val transformer = TaskJsonTransformer().withContext(taskContext)
    val result = transformer(Seq.empty).head

    // Check result
    implicit val writeContext: WriteContext[JsValue] = WriteContext.fromProject(project)
    val expectedJson = JsonSerialization.toJson[Task[TaskSpec]](dataset)
    result shouldBe expectedJson.toString()
  }

  override def workspaceProviderId: String = "inMemory"

}
