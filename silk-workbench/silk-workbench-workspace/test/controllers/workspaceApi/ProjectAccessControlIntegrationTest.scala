package controllers.workspaceApi

import controllers.projectApi.routes.ProjectApi
import controllers.util.ProjectApiClient
import controllers.workspaceApi.TestWebUserManager._
import controllers.workspaceApi.project.ProjectApiRestPayloads.{CreateProjectRequest, ItemMetaData, ProjectAccessControl}
import helper.{IntegrationTestTrait, RequestFailedException}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime
import org.silkframework.runtime.activity.{SimpleUserContext, UserContext}
import org.silkframework.runtime.plugin.PluginRegistry
import org.silkframework.runtime.plugin.annotations.Plugin
import org.silkframework.runtime.users.{User, UserActions, WebUser, WebUserManager}
import org.silkframework.serialization.json.MetaDataSerializers.MetaDataPlain
import org.silkframework.util.ConfigTestTrait
import org.silkframework.workspace.{Workspace, WorkspaceFactory}
import org.silkframework.workspace.xml.XmlZipWithResourcesProjectMarshaling
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSRequest
import play.api.mvc.{Call, RequestHeader}
import play.api.routing.Router
import sun.jvm.hotspot.debugger.cdbg.AccessControl

import scala.concurrent.Await

/**
 * Integration test for the access control of the project API.
 */
class ProjectAccessControlIntegrationTest extends AnyFlatSpec with IntegrationTestTrait with Matchers with ProjectApiClient with ConfigTestTrait {

  behavior of "Project Access Control"

  override def workspaceProviderId: String = "inMemoryWorkspaceProvider"

  override def routes: Option[Class[_ <: Router]] = Some(classOf[testWorkspace.Routes])

  override def propertyMap: Map[String, Some[String]] = {
    PluginRegistry.registerPlugin(classOf[TestWebUserManager])
    Map(
      "user.manager.web.plugin" -> Some("testWebUserManager"),
      "workspace.accessControl.enabled" -> Some("true")
    )
  }

  it should "only allow users from one of the project groups to access it" in {
    val projectId = "groupAccess"
    createProject(projectId, user1, Set(group1))
    testGetProject(projectId, user1, shouldHaveAccess = true)
    testGetProject(projectId, user2, shouldHaveAccess = false)
  }

  it should "persist project groups to the workspace backend" in {
    val projectId = "groupPersistence"
    createProject(projectId, user1, Set(group1))
    getProjectAccessControl(projectId, user1).groups shouldBe Set(group1)
    WorkspaceFactory().workspace.reload()(SimpleUserContext(admin))
    getProjectAccessControl(projectId, user1).groups shouldBe Set(group1)
  }

  it should "apply groups specified on import" in {
    val projectId = "importWithGroup"

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

  it should "allow any user to access a project with no groups assigned" in {
    val projectId = "openAccess"
    createProject(projectId, user1, Set())
    testGetProject(projectId, user1, shouldHaveAccess = true)
    testGetProject(projectId, user2, shouldHaveAccess = true)
  }

  it should "allow users from any of multiple assigned groups to access the project" in {
    val projectId = "multiGroupAccess"
    createProject(projectId, user1, Set(group1, group2))
    testGetProject(projectId, user1, shouldHaveAccess = true)
    testGetProject(projectId, user2, shouldHaveAccess = true)
  }

  it should "grant access to a previously unauthorized user after updating access control" in {
    val projectId = "accessGrant"
    createProject(projectId, user1, Set(group1))
    testGetProject(projectId, user2, shouldHaveAccess = false)
    updateProjectAccessControl(projectId, user1, Set(group1, group2)) shouldBe 200
    testGetProject(projectId, user2, shouldHaveAccess = true)
  }

  it should "revoke access after updating access control" in {
    val projectId = "accessRevoke"
    createProject(projectId, user1, Set(group1, group2))
    testGetProject(projectId, user2, shouldHaveAccess = true)
    updateProjectAccessControl(projectId, user1, Set(group1)) shouldBe 200
    testGetProject(projectId, user2, shouldHaveAccess = false)
  }

  it should "not allow an unauthorized user to update access control" in {
    val projectId = "unauthorizedUpdate"
    createProject(projectId, user1, Set(group1))
    // user2 is not in group1, so should not be allowed to update access control
    updateProjectAccessControl(projectId, user2, Set(group1, group2)) shouldBe 403
    // Access control should remain unchanged
    getProjectAccessControl(projectId, user1).groups shouldBe Set(group1)
  }

  it should "round-trip groups through export with exportGroups and import with importGroups" in {
    val projectId = "exportImportGroups"

    // Create a project with group1
    createProject(projectId, user1, Set(group1))
    getProjectAccessControl(projectId, user1).groups shouldBe Set(group1)

    // Export with exportGroups=true
    val exportedBytes = exportProject(projectId, user1, exportGroups = true)
    deleteProject(projectId, user1)

    // Import with importGroups=true — groups from archive should be preserved
    importProject(projectId, exportedBytes, user1, importGroups = true)
    getProjectAccessControl(projectId, user1).groups shouldBe Set(group1)
  }

  it should "clear groups from archive when importing with importGroups=false" in {
    val projectId = "exportClearGroups"

    // Create a project with group1
    createProject(projectId, user1, Set(group1))
    getProjectAccessControl(projectId, user1).groups shouldBe Set(group1)

    // Export with exportGroups=true (archive contains group1)
    val exportedBytes = exportProject(projectId, user1, exportGroups = true)
    deleteProject(projectId, user1)

    // Import with importGroups=false (default) — groups should be cleared
    importProject(projectId, exportedBytes, user1)
    getProjectAccessControl(projectId, user1).groups shouldBe Set.empty
  }

  it should "reject import with importGroups if archive does not contain groups" in {
    val projectId = "importGroupsMissing"

    // Create a project, export WITHOUT exportGroups, then delete
    createProject(projectId, user1, Set(group1))
    val exportedBytes = exportProject(projectId, user1)
    deleteProject(projectId, user1)

    // Import with importGroups=true — should fail because archive has no groups
    importProjectStatus(projectId, exportedBytes, user1, importGroups = true) shouldBe 400
  }

  it should "reject import with both importGroups and groups parameters" in {
    val projectId = "importGroupsConflict"

    createProject(projectId, user1, Set(group1))
    val exportedBytes = exportProject(projectId, user1)
    deleteProject(projectId, user1)

    // Import with both importGroups=true and groups — should fail with 400
    importProjectStatus(projectId, exportedBytes, user1, groups = Set(group2), importGroups = true) shouldBe 400
  }

  it should "preserve existing groups when overwriting a project without explicit groups" in {
    val projectId = "overwritePreserveGroups"

    // Create a project with group1
    createProject(projectId, user1, Set(group1))
    getProjectAccessControl(projectId, user1).groups shouldBe Set(group1)

    // Export the project (without groups in the archive)
    val exportedBytes = exportProject(projectId, user1)

    // Re-import the project with overwrite, without importGroups and without explicit groups
    val workspace = WorkspaceFactory().workspace
    val marshaller = XmlZipWithResourcesProjectMarshaling()
    val tempFile = java.io.File.createTempFile("import", ".zip")
    try {
      java.nio.file.Files.write(tempFile.toPath, exportedBytes)
      workspace.importProject(projectId, tempFile, marshaller, overwrite = true, importGroups = false, groups = Set.empty)(SimpleUserContext(admin))
    } finally {
      tempFile.delete()
    }

    // The existing project's groups should be preserved
    getProjectAccessControl(projectId, user1).groups shouldBe Set(group1)
  }

  it should "reject export with exportGroups if access control is disabled" in {
    val projectId = "exportGroupsAclDisabled"

    createProject(projectId, user1, Set(group1))

    ConfigTestTrait.withConfig("workspace.accessControl.enabled" -> Some("false")) {
      val ex = intercept[RequestFailedException] {
        exportProject(projectId, user1, exportGroups = true)
      }
      ex.response.status shouldBe 400
    }
  }

  it should "apply specified groups to a cloned project" in {
    val sourceId = "cloneSourceGroup"
    val cloneId = "cloneWithGroup"
    createProject(sourceId, user1, Set(group1))
    cloneProject(sourceId, cloneId, cloneId, user1, groups = Some(Set(group1)))
    // user2 is only in group2, so should not be able to access the clone restricted to group1
    testGetProject(cloneId, user2, shouldHaveAccess = false)
    // The clone should have group1 set
    getProjectAccessControl(cloneId, user1).groups shouldBe Set(group1)
  }

  /**
   * Exports the project with the given id and returns the exported bytes.
   */
  def exportProject(projectId: String, user: User, exportGroups: Boolean = false): Array[Byte] = {
    checkResponse(userRequest(controllers.workspace.routes.ProjectMarshalingApi.exportProject(projectId, exportGroups), user).get()).bodyAsBytes.toArray
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
  def importProject(projectId: String, projectBytes: Array[Byte], user: User, groups: Set[String] = Set.empty, importGroups: Boolean = false): Unit = {
    checkResponse(userRequest(controllers.workspace.routes.ProjectMarshalingApi.importProject(projectId, groups.toList, importGroups), user).post(projectBytes))
  }

  /** Imports a project and returns the HTTP status code instead of asserting success. */
  def importProjectStatus(projectId: String, projectBytes: Array[Byte], user: User, groups: Set[String] = Set.empty, importGroups: Boolean = false): Int = {
    Await.result(
      userRequest(controllers.workspace.routes.ProjectMarshalingApi.importProject(projectId, groups.toList, importGroups), user).post(projectBytes),
      200.seconds
    ).status
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

  /** Updates the access control groups for a project. Returns the HTTP status code. */
  def updateProjectAccessControl(projectId: String, user: User, groups: Set[String]): Int = {
    val response = Await.result(
      userRequest(ProjectApi.updateProjectAccessControl(projectId), user)
        .put(Json.toJson(ProjectAccessControl(groups))),
      200.seconds)
    response.status
  }

  /** Clones a project. */
  def cloneProject(fromProjectId: String, newProjectId: String, newLabel: String, user: User, groups: Option[Set[String]] = None): Unit = {
    val baseBody = Json.obj("metaData" -> Json.obj("label" -> newLabel), "newTaskId" -> newProjectId)
    val body = groups.fold(baseBody)(g => baseBody + ("groups" -> Json.toJson(g)))
    checkResponse(userRequest(ProjectApi.cloneProject(fromProjectId), user).post(body))
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

  val user1 = new WebUser("http://dummyUri/user1", Some("User 1"), groups = Set(group1), actions = UserActions.empty)
  val user2 = new WebUser("http://dummyUri/user2", Some("User 2"), groups = Set(group2), actions = UserActions.empty)
  val admin = new WebUser("http://dummyUri/admin", Some("Admin"), groups = Set(), actions = UserActions.all)

  val users: Seq[WebUser] = Seq(user1, user2, admin)
  val usersByUri: Map[String, WebUser] = users.map(u => u.uri -> u).toMap
}
