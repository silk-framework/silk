package controllers.linking

import controllers.autoCompletion.AutoSuggestAutoCompletionResponse
import controllers.transform.autoCompletion.PartialSourcePathAutoCompletionRequest
import helper.IntegrationTestTrait
import org.scalatest.BeforeAndAfterAll
import org.silkframework.rule.LinkSpec
import org.silkframework.serialization.json.JsonHelpers
import org.silkframework.workspace.SingleProjectWorkspaceProviderTestTrait
import org.silkframework.workspace.activity.linking.LinkingPathsCache
import play.api.libs.json.Json
import test.Routes
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class LinkingAutoCompletionApiTest extends AnyFlatSpec with SingleProjectWorkspaceProviderTestTrait with IntegrationTestTrait with Matchers with BeforeAndAfterAll {
  override def workspaceProviderId = "inMemory"

  override def projectPathInClasspath: String = "diProjects/423a27b9-c6e6-45e5-84d2-26d94fce3d1b_Partialauto-completionproject.zip"

  protected override def routes: Option[Class[Routes]] = Some(classOf[test.Routes])

  private val linkingTask = "link-rdf-and-json"

  private val rdfSourcePaths = Seq("<https://ns.eccenca.com/source/address>", "<https://ns.eccenca.com/source/age>", "<https://ns.eccenca.com/source/name>", "rdf:type")
  private val rdfSpecialPaths = Seq("#lang", "#text")
  private val rdfAllOperatorCompletions = Seq("/", "\\", "[", "[@lang = 'en']")
  private val backwardOp = Seq("\\")
  private val jsonSourcePaths = Seq("department", "id", "name", "phoneNumbers", "department/id", "department/tags", "phoneNumbers/number", "phoneNumbers/type", "department/tags/evenMoreNested", "department/tags/tagId")
  private val jsonSpecialPaths = Seq("#id", "#text", "#propertyName", "#line", "#column", "*")
  private val jsonAllOperatorCompletions = Seq("/", "\\", "[")
  private val jsonOperatorCompletions = Seq("\\..", "\\")

  private def rdfCompletionResults(inputText: String, cursorPosition: Int = 0): Seq[String] = {
    suggestedValues(partialSourcePathAutoCompleteRequest(linkingTask, isTarget = false, inputText, cursorPosition))
  }

  private def jsonCompletionResults(inputText: String, cursorPosition: Int = 0): Seq[String] = {
    suggestedValues(partialSourcePathAutoCompleteRequest(linkingTask, isTarget = true, inputText, cursorPosition))
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    project.task[LinkSpec](linkingTask).activity[LinkingPathsCache].control.waitUntilFinished()
  }

  it should "return full auto-completion results for empty query" in {
    rdfCompletionResults("") mustBe rdfSourcePaths ++ rdfSpecialPaths ++ backwardOp
    jsonCompletionResults("") mustBe jsonSourcePaths ++ jsonSpecialPaths ++ jsonOperatorCompletions
  }

  it should "return full auto-completion results for query" in {
    rdfCompletionResults("ame") mustBe rdfSourcePaths.filter(_.contains("ame")) ++ backwardOp
    jsonCompletionResults("ags") mustBe jsonSourcePaths.filter(_.contains("ags")) ++ backwardOp
  }

  it should "return partial auto-completion results" in {
    val address = "<https://ns.eccenca.com/source/address>"
    rdfCompletionResults(address + "/", address.length + 1) mustBe (rdfSourcePaths ++ rdfSpecialPaths).map(p => s"/$p")
    // Since the linking paths cache seems to contain only paths of length 1, no paths will be suggested besides special paths
    jsonCompletionResults("department/", "department/".length) mustBe (jsonSourcePaths
      .filter(_.startsWith("department/"))
      .map(_.stripPrefix("department/")) ++ jsonSpecialPaths
      ).map(p => s"/$p")
  }

  it should "return partial auto-completion results with query" in {
    val address = "<https://ns.eccenca.com/source/address>"
    rdfCompletionResults(address + "/ame", address.length + 3) mustBe rdfSourcePaths.filter(_.contains("ame")).map(p => s"/$p") ++ rdfAllOperatorCompletions
    // Since the linking paths cache seems to contain only paths of length 1, no paths will be suggested besides special paths
    jsonCompletionResults("department/ags", "department/ags".length) mustBe jsonSourcePaths
      .filter(p => p.startsWith("department/") && p.contains("ags"))
      .map(_.stripPrefix("department/"))
      .map(p => s"/$p") ++ jsonAllOperatorCompletions
  }

  private def partialSourcePathAutoCompleteRequest(linkingTaskId: String,
                                                   isTarget: Boolean,
                                                   inputText: String = "",
                                                   cursorPosition: Int = 0,
                                                   maxSuggestions: Option[Int] = None): AutoSuggestAutoCompletionResponse = {
    val partialUrl = controllers.linking.routes.LinkingAutoCompletionApi.partialSourcePath(projectId, linkingTaskId, isTarget).url
    val response = client.url(s"$baseUrl$partialUrl").post(Json.toJson(PartialSourcePathAutoCompletionRequest(inputText, cursorPosition, maxSuggestions, Some(false), None)))
    JsonHelpers.fromJsonValidated[AutoSuggestAutoCompletionResponse](checkResponse(response).json)
  }

  private def suggestedValues(result: AutoSuggestAutoCompletionResponse): Seq[String] = {
    result.replacementResults.flatMap(_.replacements.map(_.value))
  }
}
