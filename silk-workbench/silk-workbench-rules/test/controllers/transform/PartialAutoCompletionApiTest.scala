package controllers.transform

import controllers.autoCompletion.{AutoSuggestAutoCompletionResponse, CompletionBase, ReplacementInterval, ReplacementResults}
import controllers.transform.autoCompletion._
import helper.{IntegrationTestTrait, RequestFailedException}
import org.silkframework.entity.ValueType
import org.silkframework.entity.paths.TypedPath
import org.silkframework.entity.rdf.SparqlEntitySchema.specialPaths
import org.silkframework.plugins.dataset.json.JsonDataset
import org.silkframework.rule.TransformSpec
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import org.silkframework.serialization.json.JsonHelpers
import org.silkframework.workspace.SingleProjectWorkspaceProviderTestTrait
import org.silkframework.workspace.activity.transform.{CachedEntitySchemata, TransformPathsCache}
import play.api.libs.json.Json
import test.Routes

import scala.xml.{Node, XML}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.silkframework.dataset.DatasetCharacteristics.SpecialPaths

class PartialAutoCompletionApiTest extends AnyFlatSpec with Matchers with SingleProjectWorkspaceProviderTestTrait with IntegrationTestTrait {

  behavior of "Partial source path auto-complete endpoint"

  private val rdfTransform = "17fef5a5-a920-4665-92f5-cc729900e8f1_TransformRDF"
  private val jsonTransform = "2a997fb4-1bc7-4344-882e-868193568e87_TransformJSON"
  private val allJsonPaths = Seq("department", "id", "name", "phoneNumbers", "department/id", "department/tags", "phoneNumbers/number",
    "phoneNumbers/type", "department/tags/evenMoreNested", "department/tags/tagId", "department/tags/evenMoreNested/value")

  private val jsonSpecialPaths = JsonDataset.characteristics.supportedPathExpressions.specialPaths.map(_.value)
  private val jsonSpecialPathsFull = fullPaths(jsonSpecialPaths)
  private val jsonOps = Seq("/", "\\", "[")

  private val RDF_NS = "https://ns.eccenca.com/source"
  private val allPersonRdfPaths = Seq(s"<$RDF_NS/address>", s"<$RDF_NS/age>", s"<$RDF_NS/name>", "rdf:type", specialPaths.LANG, specialPaths.TEXT)
  private val allForwardRdfPaths = Seq("<https://ns.eccenca.com/source/address>", "<https://ns.eccenca.com/source/age>",
    "<https://ns.eccenca.com/source/city>", "<https://ns.eccenca.com/source/country>", "<https://ns.eccenca.com/source/name>", "rdf:type", specialPaths.LANG, specialPaths.TEXT)
  private val allBackwardRdfPaths = Seq("\\<https://ns.eccenca.com/source/address>", "\\rdf:type")
  // Full serialization of paths
  private def fullPaths(paths: Seq[String]) = paths.map(p => if(p.startsWith("\\")) p else "/" + p)
  private val rdfOps: Seq[String] = jsonOps ++ Seq("[@lang = 'en']")
  private val backwardPathTransform = "Transformwithbackwardpath_4cbd9840bc500dd9"
  private val backwardPathRuleId = "backward"

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
    jsonSuggestionsForPath("department/") mustBe fullPaths(
      allJsonPaths.filter(p => p.startsWith("department/")).map(_.stripPrefix("department/")) ++
        jsonSpecialPaths.filter(!_.startsWith("\\"))
    )
    jsonSuggestionsForPath("/") must not contain("\\..")
    jsonSuggestionsForPath("/") must contain("#text")
    jsonSuggestionsForPath("department\\") mustBe Seq("\\..")
  }

  it should "return a client error on invalid requests" in {
    intercept[RequestFailedException] {
      partialSourcePathAutoCompleteRequest(jsonTransform, cursorPosition = -1)
    }
    intercept[RequestFailedException] {
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
    jsonSuggestions(inputWithFilter.take(secondFilterStartIdx) + "ta id" + inputWithFilter.drop(secondFilterStartIdx + 2), secondFilterStartIdx) mustBe Seq("tagId", "#uuid")
    // Empty query
    jsonSuggestions(inputWithFilter.take(secondFilterStartIdx) + "" + inputWithFilter.drop(secondFilterStartIdx + 2), secondFilterStartIdx) mustBe Seq("evenMoreNested", "tagId") ++ jsonSpecialPaths
  }

  it should "auto-complete JSON paths inside started filter expressions" in {
    val inputWithFilter = """department[id = "department X"]/tags[id"""
    jsonSuggestions(inputWithFilter, inputWithFilter.length) mustBe Seq("tagId") ++ jsonSpecialPaths.filter(_.contains("id"))
  }

  it should "not suggest anything when inside quotes" in {
    val pathWithQuotes = """department[b = "some value"]"""
    jsonSuggestions(pathWithQuotes, pathWithQuotes.length - 3) mustBe Seq.empty
  }

  it should "not suggest path operators when inside a URI" in {
    rdfSuggestions("""<urn:test""") mustBe Seq.empty
    rdfSuggestions("""rdf:object[rdfs:label = <urn:test""") mustBe Seq.empty
  }

  it should "suggest all path operators for RDF sources" in {
    rdfSuggestions("", 0, None) mustBe allPersonRdfPaths ++ Seq("\\")
    // For all longer paths suggest all properties
    rdfSuggestions("rdf:type/") mustBe fullPaths(allForwardRdfPaths)
    rdfSuggestions("rdf:type/<urn:test:test>/") mustBe fullPaths(allForwardRdfPaths)
    rdfSuggestions("rdf:type/<urn:test:test>\\") mustBe fullPaths(allBackwardRdfPaths)
  }

  it should "suggest URIs based on multi line queries for RDF sources" in {
    rdfSuggestions("rdf:type/eccenca ad") mustBe fullPaths(allForwardRdfPaths).filter(_.contains("address")) ++ rdfOps
    rdfSuggestions("rdf:type\\eccenca ad") mustBe fullPaths(allBackwardRdfPaths).filter(_.contains("address")) ++ rdfOps
  }

  it should "not propose the exact same replacement if it is the only result" in {
    // The backward operator is proposed for RDF in position 0, so show the path
    rdfSuggestions("rdf:type", 0, None) mustBe Seq("rdf:type", "\\")
    // No operator is proposed here at position 1, so do not show the path
    rdfSuggestions(s"<$RDF_NS/address>", 1, None) mustBe Seq.empty
    // The operators are proposed, so the path should also be shown
    rdfSuggestions("rdf:type") mustBe Seq("rdf:type") ++ rdfOps
    // The special paths actually match "value" in the comments, that's why they show up here and /value is still proposed
    jsonSuggestionsForPath("department/tags/evenMoreNested/value") mustBe Seq("/value", "/#id", "/#uuid", "/#text", "/#line", "/#column") ++ jsonOps
    // Here, the backward operator would show up, so also show the path
    jsonSuggestions("name", 0, None) mustBe Seq("name", "#uuid", "#key", "\\")
  }

  it should "not propose path ops inside a filter" in {
    jsonSuggestionsForPath("department/[tags = ") must not contain allOf("/", "\\", "[", "]")
  }

  it should "propose end of filter op only when the filter expression will be valid" in {
    jsonSuggestionsForPath("department/[tags = ") must not contain("]")
    jsonSuggestionsForPath("department/[@lang = 'en']") must not contain("]")
    jsonSuggestionsForPath("department/[tags = <urn:test:test>") must contain("]")
    jsonSuggestionsForPath("department/[rdf:type != rdf:Type") must contain("]")
    jsonSuggestionsForPath("department/[rdf:type != \"text") must not contain("]")
    jsonSuggestionsForPath("department/[rdf:type != <urn:start") must not contain("]")
    jsonSuggestionsForPath("department/[@lang != 'de'") must contain("]")
    jsonSuggestionsForPath("department/[<http://domain.org/label> != \"label\"") must contain("]")
  }

  it should "suggest the replacement of properties where the cursor is currently at" in {
    val input = s"<$RDF_NS/address>/rdf:type"
    val secondPathOp = "/rdf:type"
    val result = partialSourcePathAutoCompleteRequest(rdfTransform, inputText = input, cursorPosition = input.length)
    result.replacementResults must have size 2
    result.replacementResults.head.replacementInterval mustBe ReplacementInterval(input.length - secondPathOp.length, secondPathOp.length)
    val resultFirstProp = partialSourcePathAutoCompleteRequest(rdfTransform, inputText = input, cursorPosition = 2)
    resultFirstProp.replacementResults must have size 1
    resultFirstProp.replacementResults.head.replacementInterval mustBe ReplacementInterval(0, input.length - secondPathOp.length)
  }

  it should "suggest replacements for path expression in URI pattern" in {
    val inputText = "urn:{department}"
    for(cursorPosition <- Seq(inputText.length - 1, inputText.length - 2)) {
      val uriPatternAutoCompletions = uriPatternAutoCompleteRequest(jsonTransform, inputText = "urn:{department}", cursorPosition = cursorPosition)
      uriPatternAutoCompletions.inputString mustBe inputText
      uriPatternAutoCompletions.cursorPosition mustBe cursorPosition
      uriPatternAutoCompletions.replacementResults must have size(2)
      val Seq(querySpecificResults, genericOperatorResults) = uriPatternAutoCompletions.replacementResults
      querySpecificResults.extractedQuery mustBe "department"
      querySpecificResults.replacementInterval mustBe ReplacementInterval("urn:{".length, "department".length)
      querySpecificResults.replacements.map(_.value) mustBe allJsonPaths.filter(_.contains("department"))
      genericOperatorResults.replacementInterval mustBe ReplacementInterval(cursorPosition, 0)
    }
  }

  it should "suggest replacements for path expressions in URI patterns with an addition object path context" in {
    val inputText = "urn:{nested}"
    val objectPathContext = "department/tags"
    val cursorPosition = inputText.length - 1
    val uriPatternAutoCompletions = uriPatternAutoCompleteRequest(jsonTransform, inputText = inputText,
      cursorPosition = cursorPosition, objectPath = Some(objectPathContext))
    val Seq(querySpecificResults, genericOperatorResults) = uriPatternAutoCompletions.replacementResults
    querySpecificResults.replacements.map(_.value) mustBe allJsonPaths.filter(_.contains("Nested")).map(_.drop(objectPathContext.length + 1))
  }

  it should "not suggest special paths that should not be used in object mapping value paths" in {
    jsonSuggestionsForPath("", Some(true)) mustBe allJsonPaths ++ jsonSpecialPaths.filterNot(p =>
      Set(JsonDataset.specialPaths.ID, JsonDataset.specialPaths.UUID, JsonDataset.specialPaths.TEXT, JsonDataset.specialPaths.KEY).contains(p)) ++ Seq("\\")
    rdfSuggestions("", Some(true)) mustBe allPersonRdfPaths.filterNot(p => Set(specialPaths.LANG, specialPaths.TEXT).contains(p)) ++ Seq("\\")
  }

  private def suggest(query: String, ruleId: String = backwardPathRuleId): Seq[String] = {
    val response = partialSourcePathAutoCompleteRequest(backwardPathTransform, ruleId, inputText = query, cursorPosition = query.length)
    response.replacementResults
      .flatMap(_.replacements)
      .map(_.value)
  }

  it should "suggest the correct paths for object mappings starting with backward paths" in {
    suggest("") mustBe allJsonPaths ++ jsonSpecialPaths ++ Seq("\\")
    suggest("nam") mustBe Seq("name", "#uuid", "#key") ++ jsonOps
  }

  it should "consider * for datasets that support this path operator" in {
    suggest("*/") mustBe allJsonPaths
      .filter(_.contains("/"))
      .map(p => p.drop(p.indexOf("/"))) ++ jsonSpecialPaths.filter(!_.startsWith("\\")).map(v => "/" + v)
    suggest("*/id") mustBe allJsonPaths
      .filter(_.contains("/"))
      .map(p => p.drop(p.indexOf("/")))
      .filter(_.toLowerCase.contains("id")) ++ Seq("/#id", "/#uuid") ++ jsonOps
    // Ignore special paths containing "value" in the description
    suggest("*/tags/*/val").filter(!_.startsWith("/#")) mustBe Seq("/value") ++ jsonOps
  }

  /** The following test changes the suggestions. Add tests before this test. */

  it should "suggest the correct path for object mappings starting with backward paths when there is a backward path in the paths cache" in {
    // Add path with backward operator containing the forward path "name" in it to the paths cache
    val pathCacheResource = project.cacheResources.child("transform").child(backwardPathTransform).get(s"pathsCache.xml")
    val xmlValue = XML.load(pathCacheResource.inputStream)
    implicit val readContext: ReadContext = ReadContext.fromProject(project)
    implicit val writeContext: WriteContext[Node] = WriteContext.fromProject[Node](project)
    val cachedEntitySchema = CachedEntitySchemata.CachedEntitySchemaXmlFormat.read(xmlValue)
    val newCachedEntitySchema = cachedEntitySchema.copy(
      configuredSchema = cachedEntitySchema.configuredSchema.copy(
        typedPaths = cachedEntitySchema.configuredSchema.typedPaths ++ Seq(
          TypedPath("\\someBackwardPathToParent/nameAfterBack", ValueType.STRING)
        )
      )
    )
    pathCacheResource.writeString(CachedEntitySchemata.CachedEntitySchemaXmlFormat.write(newCachedEntitySchema).toString())
    val pathsCache = project.task[TransformSpec](backwardPathTransform).activity[TransformPathsCache]
    pathsCache.startDirty(reloadCacheFile = true)
    pathsCache.control.waitUntilFinished()
    val cacheValue = pathsCache.control.value.get.get
    cacheValue.configuredSchema.typedPaths.find(_.toString.contains("someBackwardPathToParent")) mustBe defined
    suggest("nam", ruleId = "toParent") mustBe Seq("nameAfterBack", "#uuid", "#key") ++ jsonOps
  }

  private def partialAutoCompleteResult(inputString: String = "",
                                        cursorPosition: Int = 0,
                                        replacementResult: Seq[ReplacementResults]): AutoSuggestAutoCompletionResponse = {
    AutoSuggestAutoCompletionResponse(inputString, cursorPosition, replacementResult)
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

  private def resultWithoutCompletions(result: AutoSuggestAutoCompletionResponse): AutoSuggestAutoCompletionResponse = {
    result.copy(replacementResults = result.replacementResults.map(_.copy(replacements = null)))
  }

  private def jsonSuggestions(inputText: String, cursorPosition: Int, isObjectPath: Option[Boolean] = None): Seq[String] = {
    project.task[TransformSpec](jsonTransform).activity[TransformPathsCache].control.waitUntilFinished()
    suggestedValues(partialSourcePathAutoCompleteRequest(jsonTransform, inputText = inputText, cursorPosition = cursorPosition, isObjectPath = isObjectPath))
  }

  private def jsonSuggestionsForPath(inputText: String, isObjectPath: Option[Boolean] = None): Seq[String] = jsonSuggestions(inputText, inputText.length, isObjectPath)

  private def rdfSuggestions(inputText: String, cursorPosition: Int, isObjectPath: Option[Boolean]): Seq[String] = {
    project.task[TransformSpec](rdfTransform).activity[TransformPathsCache].control.waitUntilFinished()
    suggestedValues(partialSourcePathAutoCompleteRequest(rdfTransform, inputText = inputText, cursorPosition = cursorPosition, isObjectPath = isObjectPath))
  }

  private def rdfSuggestions(inputText: String, isObjectPath: Option[Boolean] = None): Seq[String] = rdfSuggestions(inputText, inputText.length, isObjectPath)

  private def suggestedValues(result: AutoSuggestAutoCompletionResponse): Seq[String] = {
    result.replacementResults.flatMap(_.replacements.map(_.value))
  }

  private def partialSourcePathAutoCompleteRequest(transformId: String,
                                                   ruleId: String = "root",
                                                   inputText: String = "",
                                                   cursorPosition: Int = 0,
                                                   maxSuggestions: Option[Int] = None,
                                                   isObjectPath: Option[Boolean] = None): AutoSuggestAutoCompletionResponse = {
    val partialUrl = controllers.transform.routes.AutoCompletionApi.partialSourcePath(projectId, transformId, ruleId).url
    val response = client.url(s"$baseUrl$partialUrl").post(Json.toJson(PartialSourcePathAutoCompletionRequest(inputText, cursorPosition, maxSuggestions, isObjectPath, None)))
    JsonHelpers.fromJsonValidated[AutoSuggestAutoCompletionResponse](checkResponse(response).json)
  }

  private def uriPatternAutoCompleteRequest(transformId: String,
                                            ruleId: String = "root",
                                            inputText: String = "",
                                            cursorPosition: Int = 0,
                                            maxSuggestions: Option[Int] = None,
                                            objectPath: Option[String] = None): AutoSuggestAutoCompletionResponse = {
    val uriPatternUrl = controllers.transform.routes.AutoCompletionApi.uriPattern(projectId, transformId, ruleId).url
    val response = client.url(s"$baseUrl$uriPatternUrl").post(Json.toJson(UriPatternAutoCompletionRequest(inputText, cursorPosition, maxSuggestions, objectPath, None)))
    JsonHelpers.fromJsonValidated[AutoSuggestAutoCompletionResponse](checkResponse(response).json)
  }
}