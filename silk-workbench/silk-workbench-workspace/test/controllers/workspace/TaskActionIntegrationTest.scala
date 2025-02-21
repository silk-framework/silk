package controllers.workspace

import controllers.workspace.taskApi.{TaskActionRequest, TaskActionResponse}
import helper.IntegrationTestTrait
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.dataset.operations.{DeleteFilesOperator, DeleteFilesOperatorTest}
import org.silkframework.runtime.serialization.WriteContext
import org.silkframework.serialization.json.JsonSerializers.TaskSpecJsonFormat
import org.silkframework.workspace.Project
import play.api.libs.json._

class TaskActionIntegrationTest extends AnyFlatSpec with Matchers with IntegrationTestTrait {

  behavior of "Task action endpoints"

  private val projectId = "testProject"
  private val taskId = "deleteProjectFilesTask"
  private val pluginId = "deleteProjectFiles"
  private val actionId = "dryRun"
  private val actionLabel = "Dry run"

  it should "list the available actions in the plugin description" in {
    // Retrieve plugin description
    val jsonResult = checkResponse(client.url(s"$baseUrl/api/core/plugins/$pluginId").get()).json

    // Plugin description should contain an object with all actions
    val actions = (jsonResult \ "actions").as[JsObject]
    actions.keys shouldBe Set(actionId)

    // Each action should have a label, a description and an optional icon
    val dryRunAction = (actions \ actionId).as[JsObject]
    dryRunAction \ "label" shouldBe JsDefined(JsString(actionLabel))
    (dryRunAction \ "description").isDefined shouldBe true
    dryRunAction \ "icon" shouldBe JsDefined(JsNull)
  }

  it should "allow to call an action of a plugin task" in {
    // Create a test project with some files
    val project = retrieveOrCreateProject(projectId)
    addFiles(project, Seq("file1.csv", "file2.csv"))

    // Create a task
    val deleteFilesOperator = DeleteFilesOperator(filesRegex = "file1.csv")

    // Call the action
    val taskJson = TaskSpecJsonFormat.write(deleteFilesOperator)(WriteContext.fromProject(project))
    val requestJson = Json.toJson(TaskActionRequest(Some(taskJson)))
    val responseJson = checkResponse(client.url(s"$baseUrl/workspace/projects/$projectId/tasks/$taskId/action/$actionId").post(requestJson)).json
    val response = Json.fromJson[TaskActionResponse](responseJson).get

    // Check the response
    response.message should include ("file1.csv")
    response.message should not include ("file2.csv")
  }

  private def addFiles(project: Project, fileNames: Seq[String]): Unit = {
    val resourceManager = project.resources
    for (fileName <- fileNames) {
      resourceManager.get(fileName).writeString("content")
    }
  }

  override def workspaceProviderId: String = "inMemory"

  protected override def routes = Some(classOf[testWorkspace.Routes])

}
