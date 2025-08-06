package controllers.workspaceApi

import controllers.workspaceApi.validation.{AutoSuggestValidationError, AutoSuggestValidationResponse, SourcePathValidationRequest, UriPatternValidationRequest}
import helper.IntegrationTestTrait

import org.silkframework.serialization.json.JsonHelpers
import play.api.libs.json.Json
import play.api.routing.Router

import scala.util.Try
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers


class ValidationApiTest extends AnyFlatSpec with IntegrationTestTrait with Matchers {
  behavior of "Validation API"

  val projectId = "testProject"

  override def workspaceProviderId: String = "inMemoryWorkspaceProvider"

  override def routes: Option[Class[_ <: Router]] = Some(classOf[testWorkspace.Routes])

  it should "validate the syntax of Silk source path expressions" in {
    createProject(projectId)
    validateSourcePathRequest("""/valid/path[subPath = "filter value"]""") mustBe AutoSuggestValidationResponse(true, None)
    val invalidResult = validateSourcePathRequest("""/invalid/path with spaces at the wrong place""")
    invalidResult.valid mustBe false
    invalidResult.parseError.get.copy(message = "") mustBe AutoSuggestValidationError("", "/invalid/path".length, "/invalid/path".length + 2)
  }

  it should "validate the syntax of URI patterns" in {
    Try(createProject(projectId))
    validateUriPatternRequest("""urn:{/valid/path[subPath = "filter value"]/value}""") mustBe AutoSuggestValidationResponse(true, None)
    val startOfInvalidPart = "http://example.com/{silkPath}".length
    invalidUriPattern("""http://example.com/{silkPath}/invalid path""") mustBe AutoSuggestValidationError("", startOfInvalidPart, startOfInvalidPart + "/invalid path".length)
    invalidUriPattern("urn:{invalid path}") mustBe AutoSuggestValidationError("", "urn:{invalid".length, "urn:{invalid ".length)
    invalidUriPattern("invalid") mustBe AutoSuggestValidationError("", 0, "invalid".length)
    invalidUriPattern("urn:{") mustBe AutoSuggestValidationError("", "urn:".length, "urn:{".length)
    invalidUriPattern("urn:{{") mustBe AutoSuggestValidationError("", "urn:{".length, "urn:{{".length)
  }

  private def invalidUriPattern(uriPattern: String): AutoSuggestValidationError = {
    val result = validateUriPatternRequest(uriPattern)
    result.valid mustBe false
    result.parseError.get.copy(message = "")
  }

  private def validateUriPatternRequest(uriPattern: String): AutoSuggestValidationResponse = {
    val response = client.url(s"$baseUrl/api/workspace/validation/uriPattern/$projectId")
      .post(Json.toJson(UriPatternValidationRequest(uriPattern)))
    JsonHelpers.fromJsonValidated[AutoSuggestValidationResponse](checkResponse(response).json)
  }

  private def validateSourcePathRequest(pathExpression: String): AutoSuggestValidationResponse = {
    val response = client.url(s"$baseUrl/api/workspace/validation/sourcePath/$projectId")
      .post(Json.toJson(SourcePathValidationRequest(pathExpression)))
    JsonHelpers.fromJsonValidated[AutoSuggestValidationResponse](checkResponse(response).json)
  }
}
