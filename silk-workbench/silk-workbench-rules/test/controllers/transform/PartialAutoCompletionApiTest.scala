package controllers.transform

import controllers.transform.autoCompletion.{PartialSourcePathAutoCompletionRequest, PartialSourcePathAutoCompletionResponse, ReplacementInterval}
import helper.IntegrationTestTrait
import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.serialization.json.JsonHelpers
import org.silkframework.workspace.SingleProjectWorkspaceProviderTestTrait
import play.api.libs.json.Json
import test.Routes

class PartialAutoCompletionApiTest extends FlatSpec with MustMatchers with SingleProjectWorkspaceProviderTestTrait with IntegrationTestTrait {

  behavior of "Partial source path auto-complete endpoint"

  private val rdfTransform = "17fef5a5-a920-4665-92f5-cc729900e8f1_TransformRDF"
  private val jsonTransform = "2a997fb4-1bc7-4344-882e-868193568e87_TransformJSON"

  val allJsonPaths = Seq("department", "department/id", "department/tags",
    "department/tags/evenMoreNested", "department/tags/evenMoreNested/value", "department/tags/tagId", "id", "name",
    "phoneNumbers", "phoneNumbers/number", "phoneNumbers/type")

  /**
    * Returns the path of the XML zip project that should be loaded before the test suite starts.
    */
  override def projectPathInClasspath: String = "diProjects/423a27b9-c6e6-45e5-84d2-26d94fce3d1b_Partialauto-completionproject.zip"

  override def workspaceProviderId: String = "inMemory"

  protected override def routes: Option[Class[Routes]] = Some(classOf[test.Routes])

  it should "auto-complete all paths when input is an empty string" in {
    val result = partialSourcePathAutoCompleteRequest(jsonTransform)
    result.copy(replacementResults = null) mustBe PartialSourcePathAutoCompletionResponse("", 0, Some(ReplacementInterval(0, 0)), "", null)
    suggestedValues(result) mustBe allJsonPaths
  }

  it should "auto-complete with multi-word text filter if there is a one hop path entered" in {
    val inputText = "depart id"
    val result = partialSourcePathAutoCompleteRequest(jsonTransform, inputText = inputText, cursorPosition = 2)
    result.copy(replacementResults = null) mustBe PartialSourcePathAutoCompletionResponse(inputText, 2, Some(ReplacementInterval(0, inputText.length)), inputText,null)
    suggestedValues(result) mustBe Seq("department/id", "department/tags/tagId")
  }

  it should "auto-complete JSON (also XML) paths at any level" in {
    val level1EndInput = "department/id"
    val level1Prefix = "department/"
    // Return all relative paths that match "id" for any cursor position after the first slash
    for(cursorPosition <- level1EndInput.length - 2 to level1EndInput.length) {
      jsonSuggestions(level1EndInput, cursorPosition) mustBe Seq("id", "tags/tagId")
    }
  }

  private def jsonSuggestions(inputText: String, cursorPosition: Int): Seq[String] = {
    suggestedValues(partialSourcePathAutoCompleteRequest(jsonTransform, inputText = inputText, cursorPosition = cursorPosition))
  }

  private def rdfSuggestions(inputText: String, cursorPosition: Int): Set[String] = {
    suggestedValues(partialSourcePathAutoCompleteRequest(rdfTransform, inputText = inputText, cursorPosition = cursorPosition)).toSet
  }

  private def suggestedValues(result: PartialSourcePathAutoCompletionResponse): Seq[String] = {
    result.replacementResults.completions.map(_.value)
  }

  private def partialSourcePathAutoCompleteRequest(transformId: String,
                                                   ruleId: String = "root",
                                                   inputText: String = "",
                                                   cursorPosition: Int = 0,
                                                   maxSuggestions: Option[Int] = None): PartialSourcePathAutoCompletionResponse = {
    val partialUrl = controllers.transform.routes.AutoCompletionApi.partialSourcePath(projectId, transformId, ruleId).url
    val response = client.url(s"$baseUrl$partialUrl").post(Json.toJson(PartialSourcePathAutoCompletionRequest(inputText, cursorPosition, maxSuggestions)))
    JsonHelpers.fromJsonValidated[PartialSourcePathAutoCompletionResponse](checkResponse(response).json)
  }
}
