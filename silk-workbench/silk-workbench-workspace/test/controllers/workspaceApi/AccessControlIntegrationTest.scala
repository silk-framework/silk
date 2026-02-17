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
import play.api.mvc.RequestHeader
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
    val request = withUser(createRequest(ProjectApi.createNewProject()), user)
    val response = request.post(Json.toJson(createProjectRequest))
    checkResponse(response)
  }

  /**
   * Checks that the given user can read a project.
   * Fails if the project does not exist or the user does not have access rights.
   */
  def testGetProject(projectId: String, user: User, shouldHaveAccess: Boolean): Unit = {
    val request = withUser(createRequest(ProjectApi.getProjectMetaData(projectId)), user)
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
    val request = withUser(createRequest(ProjectApi.getProjectAccessControl(projectId)), user)
    val response = Await.result(request.get(), 200.seconds)
    Json.fromJson[ProjectAccessControl](response.body[JsValue]).get
  }

  private def withUser(request: WSRequest, user: User): WSRequest = {
    request.addHttpHeaders("X-Forwarded-User" -> user.uri)
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
