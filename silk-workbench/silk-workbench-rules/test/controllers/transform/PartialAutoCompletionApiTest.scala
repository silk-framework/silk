package controllers.transform

import controllers.transform.autoCompletion._
import helper.IntegrationTestTrait
import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.plugins.dataset.json.JsonSource
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

  private val jsonSpecialPaths = JsonSource.specialPaths.all
  private val jsonSpecialPathsFull = jsonSpecialPaths.map(p => s"/$p")
  val jsonOps = Seq("/", "\\", "[")

  /**
    * Returns the path of the XML zip project that should be loaded before the test suite starts.
    */
  override def projectPathInClasspath: String = "diProjects/423a27b9-c6e6-45e5-84d2-26d94fce3d1b_Partialauto-completionproject.zip"

  override def workspaceProviderId: String = "inMemory"

  protected override def routes: Option[Class[Routes]] = Some(classOf[test.Routes])

  it should "auto-complete all paths when input is an empty string" in {
    val result = partialSourcePathAutoCompleteRequest(jsonTransform)
    resultWithoutCompletions(result) mustBe partialAutoCompleteResult(replacementResult = Seq(
      replacementResults(completions = null),
      replacementResults(completions = null)
    ))
    // Do not propose filter or forward operators at the beginning of a path. Filters are currently invalid in first position (FIXME: CMEM-226).
    suggestedValues(result) mustBe allJsonPaths ++ jsonSpecialPaths ++ Seq("\\")
    // Path ops don't count for maxSuggestions
    suggestedValues(partialSourcePathAutoCompleteRequest(jsonTransform, maxSuggestions = Some(1))) mustBe allJsonPaths.take(1) ++ Seq("\\")
  }

  it should "auto-complete with multi-word text filter if there is a one hop path entered" in {
    val inputText = "depart id"
    val cursorPosition = 2
    val result = partialSourcePathAutoCompleteRequest(jsonTransform, inputText = inputText, cursorPosition = cursorPosition)
    resultWithoutCompletions(result) mustBe partialAutoCompleteResult(inputText, cursorPosition, replacementResult = Seq(
      replacementResults(0, inputText.length, inputText, completions = null),
      replacementResults(cursorPosition, 0, "", completions = null)
    ))
    suggestedValues(result) mustBe Seq("department/id", "department/tags/tagId") ++ jsonOps
  }

  it should "auto-complete JSON (also XML) paths at any level" in {
    val level1EndInput = "department/id"
    // Return all relative paths that match "id" for any cursor position after the first slash
    for(cursorPosition <- level1EndInput.length - 2 to level1EndInput.length) {
      val opsSegments = if(level1EndInput(cursorPosition - 1) != '/') jsonOps else Seq.empty
      jsonSuggestions(level1EndInput, cursorPosition) mustBe Seq("/id", "/tags/tagId") ++ jsonSpecialPathsFull.filter(_.contains("id")) ++ opsSegments
    }
    val level2EndInput = "department/tags/id"
    for(cursorPosition <- level2EndInput.length - 2 to level2EndInput.length) {
      val opsSegments = if(level2EndInput(cursorPosition - 1) != '/')  jsonOps else Seq.empty
      jsonSuggestions(level2EndInput, cursorPosition) mustBe Seq("/tagId") ++ jsonSpecialPathsFull.filter(_.contains("id")) ++ opsSegments
    }
  }

  it should "return a client error on invalid requests" in {
    intercept[AssertionError] {
      partialSourcePathAutoCompleteRequest(jsonTransform, cursorPosition = -1)
    }
    intercept[AssertionError] {
      partialSourcePathAutoCompleteRequest(jsonTransform, maxSuggestions = Some(0))
    }
  }

  it should "auto-complete JSON paths that have backward paths in the path prefix" in {
    val inputPath = "department/tags\\../id"
    jsonSuggestions(inputPath, inputPath.length) mustBe Seq("/id", "/tags/tagId") ++ jsonSpecialPathsFull.filter(_.contains("id")) ++ jsonOps
  }

  it should "auto-complete JSON paths inside filter expressions" in {
    val inputWithFilter = """department[id = "department X"]/tags[id = ""]/tagId"""
    val filterStartIdx = "department[".length
    val secondFilterStartIdx = """department[id = "department X"]/tags[""".length
    jsonSuggestions(inputWithFilter, filterStartIdx + 2) mustBe Seq("id") ++ jsonSpecialPaths.filter(_.contains("id"))
    jsonSuggestions(inputWithFilter, filterStartIdx) mustBe Seq("id") ++ jsonSpecialPaths.filter(_.contains("id"))
    jsonSuggestions(inputWithFilter, secondFilterStartIdx) mustBe Seq("tagId") ++ jsonSpecialPaths.filter(_.contains("id"))
    // Multi word query
    jsonSuggestions(inputWithFilter.take(secondFilterStartIdx) + "ta id" + inputWithFilter.drop(secondFilterStartIdx + 2), secondFilterStartIdx) mustBe Seq("tagId")
    // Empty query
    jsonSuggestions(inputWithFilter.take(secondFilterStartIdx) + "" + inputWithFilter.drop(secondFilterStartIdx + 2), secondFilterStartIdx) mustBe Seq("evenMoreNested", "tagId") ++ jsonSpecialPaths
  }

  it should "auto-complete JSON paths inside started filter expressions" in {
    val inputWithFilter = """department[id = "department X"]/tags[id"""
    jsonSuggestions(inputWithFilter, inputWithFilter.length) mustBe Seq("tagId") ++ jsonSpecialPaths.filter(_.contains("id"))
  }

  private def partialAutoCompleteResult(inputString: String = "",
                                        cursorPosition: Int = 0,
                                        replacementResult: Seq[ReplacementResults]): PartialSourcePathAutoCompletionResponse = {
    PartialSourcePathAutoCompletionResponse(inputString, cursorPosition, replacementResult)
  }

  private def replacementResults(from: Int = 0,
                                 to: Int = 0,
                                 query: String = "",
                                 completions: Seq[CompletionBase]
                                ): ReplacementResults = ReplacementResults(
    ReplacementInterval(from, to),
    query,
    completions
  )

  private def resultWithoutCompletions(result: PartialSourcePathAutoCompletionResponse): PartialSourcePathAutoCompletionResponse = {
    result.copy(replacementResults = result.replacementResults.map(_.copy(replacements = null)))
  }

  private def jsonSuggestions(inputText: String, cursorPosition: Int): Seq[String] = {
    suggestedValues(partialSourcePathAutoCompleteRequest(jsonTransform, inputText = inputText, cursorPosition = cursorPosition))
  }

  private def rdfSuggestions(inputText: String, cursorPosition: Int): Set[String] = {
    suggestedValues(partialSourcePathAutoCompleteRequest(rdfTransform, inputText = inputText, cursorPosition = cursorPosition)).toSet
  }

  private def suggestedValues(result: PartialSourcePathAutoCompletionResponse): Seq[String] = {
    result.replacementResults.flatMap(_.replacements.map(_.value))
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
