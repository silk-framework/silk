package controllers.workspaceApi

import controllers.projectApi.routes.ProjectApi
import controllers.util.ProjectApiClient
import controllers.workspaceApi.TestWebUserManager._
import controllers.workspaceApi.project.ProjectApiRestPayloads.{CreateProjectRequest, ItemMetaData, ProjectAccessControl}
import helper.IntegrationTestTrait
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime
import org.silkframework.runtime.activity.{SimpleUserContext, UserContext}
import org.silkframework.runtime.plugin.PluginRegistry
import org.silkframework.runtime.plugin.annotations.Plugin
import org.silkframework.runtime.users.{User, WebUser, WebUserManager}
import org.silkframework.serialization.json.MetaDataSerializers.MetaDataPlain
import org.silkframework.util.ConfigTestTrait
import org.silkframework.workspace.{Workspace, WorkspaceFactory}
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSRequest
import play.api.mvc.{Call, RequestHeader}
import play.api.routing.Router
import sun.jvm.hotspot.debugger.cdbg.AccessControl

import scala.concurrent.Await

/**
 * Integration test for the access control of the project API.
 */
class AccessControlIntegrationTest extends AnyFlatSpec with IntegrationTestTrait with Matchers with ProjectApiClient with ConfigTestTrait {

  behavior of "Project Access Control"

  override def workspaceProviderId: String = "inMemoryWorkspaceProvider"

  override def routes: Option[Class[_ <: Router]] = Some(classOf[testWorkspace.Routes])

  override def propertyMap: Map[String, Some[String]] = {
    PluginRegistry.registerPlugin(classOf[TestWebUserManager])
    Map(
      "user.manager.web.plugin" -> Some("testWebUserManager"),
    )
  }

  it should "only allow users from one of the project groups to access it" in {
    createProject("project1", user1, Set(group1))
    testGetProject("project1", user1, shouldHaveAccess = true)
    testGetProject("project1", user2, shouldHaveAccess = false)
  }

  it should "persist project groups to the workspace backend" in {
    createProject("project2", user1, Set(group1))
    getProjectAccessControl("project2", user1).groups shouldBe Set(group1)
    WorkspaceFactory().workspace.reload()
    getProjectAccessControl("project2", user1).groups shouldBe Set(group1)
  }

  it should "apply groups specified on import, not groups from the exported project" in {
    val projectId = "project3"

    // Create a new project with group 1
    createProject(projectId, user1, Set(group1))
    getProjectAccessControl(projectId, user1).groups shouldBe Set(group1)

    // Export the project and delete it
    val exportedBytes = exportProject(projectId, user1)
    deleteProject(projectId, user1)

    // Import the project with group 2
    importProject(projectId, exportedBytes, user2, groups = Set(group2))
    getProjectAccessControl(projectId, user2).groups shouldBe Set(group2)
  }

  /**
   * Exports the project with the given id and returns the exported bytes.
   */
  def exportProject(projectId: String, user: User): Array[Byte] = {
    checkResponse(userRequest(controllers.workspace.routes.ProjectMarshalingApi.exportProject(projectId), user).get()).bodyAsBytes.toArray
  }

  /**
   * Deletes the project with the given id.
   */
  def deleteProject(projectId: String, user: User): Unit = {
    checkResponse(userRequest(controllers.workspace.routes.WorkspaceApi.deleteProject(projectId), user).delete())
  }

  /**
   * Imports a project from the given bytes.
   */
  def importProject(projectId: String, projectBytes: Array[Byte], user: User, groups: Set[String] = Set.empty): Unit = {
    checkResponse(userRequest(controllers.workspace.routes.ProjectMarshalingApi.importProject(projectId, groups.toList), user).post(projectBytes))
  }

  /**
   * Creates a new project with the given id and initial groups.
   */
  def createProject(projectId: String, user: User, groups: Set[String]): Unit = {
    val createProjectRequest =
      CreateProjectRequest(
        metaData = ItemMetaData(label = projectId, description = None, tags = None),
        id = Some(projectId),
        groups = Some(groups),
      )
    val request = userRequest(ProjectApi.createNewProject(), user)
    val response = request.post(Json.toJson(createProjectRequest))
    checkResponse(response)
  }

  /**
   * Checks that the given user can read a project.
   * Fails if the project does not exist or the user does not have access rights.
   */
  def testGetProject(projectId: String, user: User, shouldHaveAccess: Boolean): Unit = {
    val request = userRequest(ProjectApi.getProjectMetaData(projectId), user)
    val response = Await.result(request.get(), 200.seconds)

    // Check the response code
    if(shouldHaveAccess) {
      response.status shouldBe 200

      // The returned project label should be the same as the requested project id.
      val metaData = Json.fromJson[MetaDataPlain](response.body[JsValue]).get.toMetaData
      metaData.label.get shouldBe projectId
    } else {
      response.status shouldBe 403
    }
  }

  def getProjectAccessControl(projectId: String, user: User): ProjectAccessControl = {
    val response = checkResponse(userRequest(ProjectApi.getProjectAccessControl(projectId), user).get())
    Json.fromJson[ProjectAccessControl](response.body[JsValue]).get
  }

  private def userRequest(call: Call, user: User): WSRequest = {
    createRequest(call).addHttpHeaders("X-Forwarded-User" -> user.uri)
  }
}

@Plugin(
  id = "testWebUserManager",
  label = "Test dummy user manager",
  description = "A web user manager for testing."
)
class TestWebUserManager extends WebUserManager {
  override def user(request: RequestHeader): Option[WebUser] = {
    val userHeader = request.headers.get("X-Forwarded-User")
    userHeader.map { user =>
      usersByUri(user)
    }
  }

  override def userContext(request: RequestHeader): UserContext = SimpleUserContext(user(request))
}

object TestWebUserManager {

  val group1 = "testGroup"
  val group2 = "otherGroup"

  val user1 = new WebUser("http://dummyUri/user1", Some("Dummy User 1"), groups = Set(group1))
  val user2 = new WebUser("http://dummyUri/user2", Some("Dummy User 2"), groups = Set(group2))

  val users = Seq(user1, user2)
  val usersByUri: Map[String, WebUser] = users.map(u => u.uri -> u).toMap
}
