package controllers.workspaceApi

import controllers.workspace.workspaceRequests.{VocabularyLookupRequest, VocabularyLookupResponse}
import helper.IntegrationTestTrait
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.silkframework.plugins.dataset.rdf.vocab.InMemoryVocabularyManager
import org.silkframework.serialization.json.JsonHelpers
import org.silkframework.util.ConfigTestTrait
import org.silkframework.workspace.WorkspaceFactory
import org.silkframework.workspace.activity.vocabulary.GlobalVocabularyCache
import play.api.libs.json.Json
import play.api.routing.Router
import play.api.test.Helpers.BAD_REQUEST

import java.io.File

class WorkspaceVocabularyApiTest extends AnyFlatSpec with ConfigTestTrait with IntegrationTestTrait with Matchers {
  behavior of "Workspace vocabulary API"

  override def workspaceProviderId: String = "inMemoryWorkspaceProvider"

  override def routes: Option[Class[_ <: Router]] = Some(classOf[testWorkspace.Routes])

  override def propertyMap: Map[String, Option[String]] = Map(
    "vocabulary.manager.plugin" -> Some("inMemoryVocabularyManager")
  )

  private val projectId = "vocabularyLookupProject"

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    createProject(projectId)
    addProjectPrefixes(projectId, Map("foaf" -> "http://xmlns.com/foaf/0.1/"))
    InMemoryVocabularyManager.addVocabulary(foafVocabularyFile)
    WorkspaceFactory().workspace.activity[GlobalVocabularyCache].control.startBlocking()
  }

  it should "resolve prefixed classes and properties from the global vocabulary cache" in {
    val response = lookup(Seq("foaf:Person", "foaf:name"))

    response.results must have size 2
    response.results.head mustBe
      response.results.head.copy(
        input = "foaf:Person",
        resolved = true,
        invalid = false,
        uri = "http://xmlns.com/foaf/0.1/Person",
        kind = Some("class"),
        label = Some("Person"),
        description = Some("A person."),
        prefixedUri = Some("foaf:Person"),
        graphUri = Some("http://xmlns.com/foaf/0.1/")
      )
    response.results(1) mustBe
      response.results(1).copy(
        input = "foaf:name",
        resolved = true,
        invalid = false,
        uri = "http://xmlns.com/foaf/0.1/name",
        kind = Some("property"),
        label = Some("name"),
        description = Some("A name for some thing."),
        prefixedUri = Some("foaf:name"),
        graphUri = Some("http://xmlns.com/foaf/0.1/")
      )
  }

  it should "mark valid but unknown absolute URIs as unresolved without being invalid" in {
    val response = lookup(Seq("http://example.org/unknown"))

    response.results mustBe Seq(
      response.results.head.copy(
        input = "http://example.org/unknown",
        resolved = false,
        invalid = false,
        uri = "http://example.org/unknown",
        kind = None,
        label = None,
        description = None,
        prefixedUri = None,
        graphUri = None
      )
    )
  }

  it should "mark non-uri-like and unknown-prefixed values as invalid" in {
    val response = lookup(Seq("some/non-uri-path", "missing:Value"))

    response.results mustBe Seq(
      response.results.head.copy(
        input = "some/non-uri-path",
        resolved = false,
        invalid = true,
        uri = "some/non-uri-path",
        kind = None,
        label = None,
        description = None,
        prefixedUri = None,
        graphUri = None
      ),
      response.results(1).copy(
        input = "missing:Value",
        resolved = false,
        invalid = true,
        uri = "missing:Value",
        kind = None,
        label = None,
        description = None,
        prefixedUri = None,
        graphUri = None
      )
    )
  }

  it should "return bad request for malformed JSON bodies" in {
    val response = client.url(s"$baseUrl${controllers.workspaceApi.routes.WorkspaceVocabularyApi.lookup().url}")
      .post(Json.obj("projectId" -> projectId))

    checkResponseExactStatusCode(response, BAD_REQUEST)
  }

  private def lookup(uris: Seq[String]): VocabularyLookupResponse = {
    val response = client.url(s"$baseUrl${controllers.workspaceApi.routes.WorkspaceVocabularyApi.lookup().url}")
      .post(Json.toJson(VocabularyLookupRequest(Some(projectId), uris)))
    JsonHelpers.fromJsonValidated[VocabularyLookupResponse](checkResponse(response).json)
  }

  private def foafVocabularyFile: File = {
    new File(getClass.getClassLoader.getResource("controllers/transform/foaf.rdf").toURI)
  }
}
