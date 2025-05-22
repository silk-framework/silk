package controllers.workspaceApi

import controllers.projectApi.ProjectApi.{CreateTag, CreateTagsRequest}
import controllers.util.ProjectApiClient
import controllers.workspaceApi.project.ProjectApiRestPayloads.{ItemMetaData, ProjectCreationData}
import helper.IntegrationTestTrait

import org.silkframework.config.MetaData
import org.silkframework.runtime.serialization.{ReadContext, TestReadContext}
import org.silkframework.serialization.json.JsonSerializers
import org.silkframework.serialization.json.JsonSerializers._
import org.silkframework.serialization.json.MetaDataSerializers.{FullTag, MetaDataPlain}
import play.api.libs.json.{JsResult, Json}
import play.api.libs.ws.WSResponse
import play.api.routing.Router
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers


class ProjectApiTest extends AnyFlatSpec with IntegrationTestTrait with Matchers with ProjectApiClient {
  behavior of "Project API"

  private def projects: Seq[String] = workspaceProvider.readProjects().map(_.id.toString)

  override def workspaceProviderId: String = "inMemory"

  override def routes: Option[Class[_ <: Router]] = Some(classOf[testWorkspace.Routes])

  lazy val projectsUrl: String = controllers.projectApi.routes.ProjectApi.createNewProject().url
  def projectPrefixesUrl(projectId: String): String = controllers.projectApi.routes.ProjectApi.fetchProjectPrefixes(projectId).url
  def projectPrefixUrl(projectId: String, prefixName: String): String = controllers.projectApi.routes.ProjectApi.addProjectPrefix(projectId, prefixName).url
  private def projectsMetaDataUrl(projectId: String): String = controllers.projectApi.routes.ProjectApi.updateProjectMetaData(projectId).url
  implicit val readContext: ReadContext = TestReadContext()

  it should "allow to create a new project by label" in {
    val projectIds = for(i <- 1 to 10) yield {
      val (label, description) = (s"Some project!", Some(s"Project description $i"))
      val response: WSResponse = createProjectByLabel(label, description)
      val metaData = Json.fromJson[MetaDataPlain]((response.json \ "metaData").get).get
      metaData.label mustBe Some(label)
      metaData.description mustBe description
      val locationHeader = response.header("Location")
      val projectId = (response.json \ "name").get.as[String]
      locationHeader mustBe defined
      val location = locationHeader.get.stripPrefix(projectsUrl + "/")
      projectId mustBe location
      projectId
    }
    val expectedProjectPrefixId = "Someproject"
    projectIds foreach { id => id must include (expectedProjectPrefixId)}
    projects foreach { id => id must include (expectedProjectPrefixId)}
  }

  it should "generate default IDs for labels without any allowed chars in IDs" in {
    (createProjectByLabel("Ä*#").json \ "name").as[String] must include ("project")
    (createProjectByLabel("!§$%").json \ "name").as[String] must include ("project")
  }

  it should "update the meta data of an existing project" in {
    val projectId = (createProjectByLabel("will be overwritten", Some("will also be overwritten")).json \ "name").as[String]
    val (newLabel, newDescription) = ("new label", Some("new description"))
    val response = client.url(s"$baseUrl${projectsMetaDataUrl(projectId)}").put(Json.toJson(ItemMetaData(newLabel, newDescription)))
    val metaDataResponse = Json.fromJson[MetaDataPlain](checkResponse(response).json).get.toMetaData
    val metaData = retrieveOrCreateProject(projectId).config.metaData
    metaData mustBe metaDataResponse
    metaData.label mustBe Some(newLabel)
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

    // Add project prefix
    val prefixName = "new"
    val prefixUri = "http://uri"
    val responseJsonPut1 = checkResponse(client.url(s"$baseUrl${projectPrefixUrl(prefixProjectId, prefixName)}").put(Json.toJson(prefixUri))).json
    (responseJsonPut1 \\ prefixName).headOption.map(_.as[String]) mustBe Some(prefixUri)
    retrieveOrCreateProject(prefixProjectId).config.prefixes.get(prefixName) mustBe Some(prefixUri)

    // Update project prefix
    val newPrefixUri = "http://newUri"
    val responseJsonPut2 = checkResponse(client.url(s"$baseUrl${projectPrefixUrl(prefixProjectId, prefixName)}").put(Json.toJson(newPrefixUri))).json
    (responseJsonPut2 \\ prefixName).headOption.map(_.as[String]) mustBe Some(newPrefixUri)
    retrieveOrCreateProject(prefixProjectId).config.prefixes.get(prefixName) mustBe Some(newPrefixUri)

    // Delete project prefix
    val responseJsonDelete = checkResponse(client.url(s"$baseUrl${projectPrefixUrl(prefixProjectId, prefixName)}").delete()).json
    (responseJsonDelete \\ prefixName).headOption must not be defined
    retrieveOrCreateProject(prefixProjectId).config.prefixes.get(prefixName) must not be defined

    // Invalid prefix value should 400
    checkResponseExactStatusCode(client.url(s"$baseUrl${projectPrefixUrl(prefixProjectId, "prefixName")}").put(Json.toJson("urn:with space")), BAD_REQUEST)
  }

  it should "support managing tags" in {
    val projectId = "tagsProject"
    createProject(projectId)

    // Make sure that initially there are no tags
    retrieveTags(projectId).tags mustBe empty

    // Add a new user-defined tag
    val tag1 = createTags(projectId, CreateTagsRequest(Seq(CreateTag(None, " My    Tag  ")))).head
    retrieveTags(projectId).tags.head.label mustBe "My Tag"

    // Update metadata
    val currentMetaData = getMetaData(projectId)
    val updatedMetaData = currentMetaData.copy(tags = Some(Set(tag1.uri)))
    updateMetaData(projectId, updatedMetaData)

    // Make sure that the tag has been added
    getMetaDataExpanded(projectId).tags must contain theSameElementsAs Set(tag1)
  }

  it should "support searching tags" in {
    val projectId = "tagsProject2"
    createProject(projectId)

    val newTags = Seq(
      CreateTag(None, "tag 1"),
      CreateTag(None, "x tag"),
      CreateTag(None, "tag a"),
      CreateTag(None, "a tag"),
      CreateTag(None, "tag X")
    )
    createTags(projectId, CreateTagsRequest(newTags))

    retrieveTags(projectId).tags.map(_.label) must contain theSameElementsInOrderAs
      Seq("a tag", "tag 1", "tag a", "tag X", "x tag")

    retrieveTags(projectId, Some("x")).tags.map(_.label) must contain theSameElementsInOrderAs
      Seq("x tag", "tag X")

    retrieveTags(projectId, Some("a")).tags.map(_.label) must contain theSameElementsInOrderAs
      Seq("a tag", "tag 1", "tag a", "tag X", "x tag")
  }

  private def fetchPrefixes: JsResult[Map[String, String]] = {
    val responseJson = checkResponse(client.url(s"$baseUrl${projectPrefixesUrl(prefixProjectId)}").get()).json
    Json.fromJson[Map[String, String]](responseJson)
  }

  private def createProjectByLabel(label: String, description: Option[String] = None, id: Option[String] = None): WSResponse = {
    val responseFuture = client.url(s"$baseUrl$projectsUrl").post(Json.toJson(ProjectCreationData(ItemMetaData(label, description), id)))
    val response = checkResponse(responseFuture)
    response
  }
}
