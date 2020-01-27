package controllers.workspaceApi

import controllers.workspaceApi.project.ProjectApiRestPayloads.{ItemMetaData, ProjectCreationData}
import helper.IntegrationTestTrait
import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.config.MetaData
import org.silkframework.runtime.serialization.ReadContext
import org.silkframework.serialization.json.JsonSerializers
import org.silkframework.serialization.json.JsonSerializers._
import play.api.libs.json.{JsResult, Json}
import play.api.libs.ws.WSResponse
import play.api.routing.Router

class ProjectApiTest extends FlatSpec with IntegrationTestTrait with MustMatchers {
  behavior of "Project API"

  private def projects: Seq[String] = workspaceProvider.readProjects().map(_.id.toString)

  override def workspaceProviderId: String = "inMemory"

  override def routes: Option[Class[_ <: Router]] = Some(classOf[test.Routes])

  lazy val projectsUrl: String = controllers.workspaceApi.routes.ProjectApi.createNewProject().url
  def projectPrefixesUrl(projectId: String): String = controllers.workspaceApi.routes.ProjectApi.fetchProjectPrefixes(projectId).url
  def projectPrefixUrl(projectId: String, prefixName: String): String = controllers.workspaceApi.routes.ProjectApi.addProjectPrefix(projectId, prefixName).url
  private def projectsMetaDataUrl(projectId: String): String = controllers.workspaceApi.routes.ProjectApi.updateProjectMetaData(projectId).url
  implicit val readContext: ReadContext = ReadContext()

  it should "allow to create a new project by label" in {
    val projectIds = for(i <- 1 to 10) yield {
      val (label, description) = (s"Some project!", Some(s"Project description $i"))
      val response: WSResponse = createProjectByLabel(label, description)
      val metaData = JsonSerializers.fromJson[MetaData]((response.json \ "metaData").get)
      metaData.label mustBe label
      metaData.description mustBe description
      val locationHeader = response.header("Location")
      val projectId = (response.json \ "name").get.as[String]
      locationHeader mustBe defined
      val location = locationHeader.get.stripPrefix(projectsUrl + "/")
      projectId mustBe location
      projectId
    }
    val expectedProjectIds = Seq("Someproject") ++ (for(i <- 2 to 10) yield s"Someproject$i")
    projectIds.sorted mustBe expectedProjectIds.sorted
    projects.sorted mustBe expectedProjectIds.sorted
  }

  it should "generate default IDs for labels without any allowed chars in IDs" in {
    (createProjectByLabel("Ä*#").json \ "name").as[String] mustBe "project"
    (createProjectByLabel("!§$%").json \ "name").as[String] mustBe "project2"
  }

  it should "update the meta data of an existing project" in {
    val projectId = (createProjectByLabel("will be overwritten", Some("will also be overwritten")).json \ "name").as[String]
    val (newLabel, newDescription) = ("new label", Some("new description"))
    val response = client.url(s"$baseUrl${projectsMetaDataUrl(projectId)}").put(Json.toJson(ItemMetaData(newLabel, newDescription)))
    checkResponse(response)
    val metaData = retrieveOrCreateProject(projectId).config.metaData
    metaData.label mustBe newLabel
    metaData.description mustBe newDescription
  }

  val prefixProjectId = "prefixProject"

  it should "fetch the project prefixes" in {
    createProject(prefixProjectId)
    val prefixes = fetchPrefixes
    prefixes.isSuccess mustBe true
    prefixes.get.size must be > 0
  }

  it should "Update project prefixes" in {
    retrieveOrCreateProject(prefixProjectId)
    val prefixes = fetchPrefixes
    val prefixName = prefixes.get.head._1
    val responseJsonDelete = checkResponse(client.url(s"$baseUrl${projectPrefixUrl(prefixProjectId, prefixName)}").delete()).json
    (responseJsonDelete \\ prefixName).headOption must not be defined
    retrieveOrCreateProject(prefixProjectId).config.prefixes.get(prefixName) must not be defined
    val newPrefixUri = "http://newUri"
    val responseJsonPut = checkResponse(client.url(s"$baseUrl${projectPrefixUrl(prefixProjectId, prefixName)}").put(Json.toJson(newPrefixUri))).json
    (responseJsonPut \\ prefixName).headOption.map(_.as[String]) mustBe Some(newPrefixUri)
    retrieveOrCreateProject(prefixProjectId).config.prefixes.get(prefixName) mustBe Some(newPrefixUri)
  }

  private def fetchPrefixes: JsResult[Map[String, String]] = {
    val responseJson = checkResponse(client.url(s"$baseUrl${projectPrefixesUrl(prefixProjectId)}").get()).json
    Json.fromJson[Map[String, String]](responseJson)
  }

  private def createProjectByLabel(label: String, description: Option[String] = None): WSResponse = {
    val responseFuture = client.url(s"$baseUrl$projectsUrl").post(Json.toJson(ProjectCreationData(ItemMetaData(label, description))))
    val response = checkResponse(responseFuture)
    response
  }
}
