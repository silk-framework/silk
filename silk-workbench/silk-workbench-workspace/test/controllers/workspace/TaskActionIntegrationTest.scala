package controllers.workspace

import controllers.workspace.taskApi.{TaskActionRequest, TaskActionResponse}
import helper.IntegrationTestTrait
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.dataset.operations.DeleteFilesOperator
import org.silkframework.runtime.serialization.WriteContext
import org.silkframework.serialization.json.JsonSerializers.TaskSpecJsonFormat
import org.silkframework.workspace.Project
import play.api.libs.json._

class TaskActionIntegrationTest extends AnyFlatSpec with Matchers with IntegrationTestTrait {

  behavior of "Task action endpoints"

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

  it should "allow to call an action on an existing project task" in {
    // Create a project with two files
    val project = createTestProject("testProject2", Seq("file1.csv", "file2.csv"))

    // Add a task to the project
    project.addTask(taskId, DeleteFilesOperator(filesRegex = "file2.csv"))

    // Call the action and check response
    val response = callAction(project.id, TaskActionRequest(task = None))
    response.message.get should not include ("file1.csv")
    response.message.get should include ("file2.csv")
  }

  it should "allow to call an action on a task that is provided in the request" in {
    // Create a project with two files
    val project = createTestProject("testProject1", Seq("file1.csv", "file2.csv"))

    // Create a task, but don't add it to the project
    val deleteFilesOperator = DeleteFilesOperator(filesRegex = "file1.csv")
    val taskJson = TaskSpecJsonFormat.write(deleteFilesOperator)(WriteContext.fromProject(project))

    // Call the action and check response
    val response = callAction(project.id, TaskActionRequest(Some(taskJson)))
    response.message.get should include ("file1.csv")
    response.message.get should not include ("file2.csv")
  }

  private def createTestProject(projectId: String, fileNames: Seq[String]): Project = {
    val project = retrieveOrCreateProject(projectId)
    val resourceManager = project.resources
    for (fileName <- fileNames) {
      resourceManager.get(fileName).writeString("content")
    }
    project
  }

  private def callAction(projectId: String, request: TaskActionRequest): TaskActionResponse = {
    val responseJson = checkResponse(client.url(s"$baseUrl/workspace/projects/$projectId/tasks/$taskId/action/$actionId").post(Json.toJson(request))).json
    Json.fromJson[TaskActionResponse](responseJson).get
  }



  override def workspaceProviderId: String = "inMemory"

  protected override def routes = Some(classOf[testWorkspace.Routes])

}
