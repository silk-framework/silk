package controllers.workspaceApi

import controllers.workspaceApi.validation.{SourcePathValidationRequest, SourcePathValidationResponse}
import helper.IntegrationTestTrait
import org.scalatest.{BeforeAndAfterAll, FlatSpec, MustMatchers}
import org.silkframework.entity.paths.PartialParseError
import org.silkframework.serialization.json.JsonHelpers
import play.api.libs.json.Json
import play.api.routing.Router

class ValidationApiTest extends FlatSpec with IntegrationTestTrait with MustMatchers {
  behavior of "Validation API"

  val projectId = "testProject"

  override def workspaceProviderId: String = "inMemory"

  override def routes: Option[Class[_ <: Router]] = Some(classOf[testWorkspace.Routes])

  it should "validate the syntax of Silk source path expressions" in {
    createProject(projectId)
    validateSourcePathRequest("""/valid/path[subPath = "filter value"]""") mustBe SourcePathValidationResponse(true, None)
    val invalidResult = validateSourcePathRequest("""/invalid/path with spaces at the wrong place""")
    invalidResult.valid mustBe false
    invalidResult.parseError.get.copy(message = "") mustBe PartialParseError("/invalid/path".length, "", " ", "/invalid/path".length)
  }

  private def validateSourcePathRequest(pathExpression: String): SourcePathValidationResponse = {
    val response = client.url(s"$baseUrl/api/workspace/validation/sourcePath/$projectId")
      .post(Json.toJson(SourcePathValidationRequest(pathExpression)))
    JsonHelpers.fromJsonValidated[SourcePathValidationResponse](checkResponse(response).json)
  }
}
