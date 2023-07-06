package controllers.transform

import controllers.linking.evaluation.EvaluateCurrentLinkageRuleRequest.EvaluationLinkFilterEnum
import controllers.linking.evaluation.{EvaluateCurrentLinkageRuleRequest, LinkRuleEvaluationStats}

import java.time.Instant
import controllers.util.SerializationUtils
import helper.IntegrationTestTrait
import org.scalatestplus.play.PlaySpec
import org.silkframework.config.MetaData
import org.silkframework.entity.Link
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.rule.{LinkSpec, LinkageRule}
import org.silkframework.rule.input.PathInput
import org.silkframework.rule.plugins.distance.equality.EqualityMetric
import org.silkframework.rule.similarity.Comparison
import org.silkframework.runtime.serialization.{ReadContext, TestReadContext, TestWriteContext, WriteContext}
import org.silkframework.serialization.json.JsonSerializers.LinkageRuleJsonFormat
import org.silkframework.serialization.json.LinkingSerializers.LinkJsonFormat
import org.silkframework.util.DPair
import org.silkframework.workspace.activity.linking.EvaluateLinkingActivity
import play.api.libs.json.{JsArray, JsValue, Json}

import java.net.URLEncoder

class LinkingTaskApiTest extends PlaySpec with IntegrationTestTrait {

  private val project = "project"
  private val task = "linking"
  private val sourceDataset = "sourceDs"
  private val targetDataset = "targetDs"
  private val outputDataset = "outputDs"
  private val csvResource = "simple.csv"
  private val csvSource1 = "csvSource1"
  private val csvSource2 = "csvSource2"
  private val csvLinkingTask = "csvLinking"
  private val outputCsvResource = "output.csv"
  private val outputCsv = "outputCsv"

  private val metaData =
    MetaData(
      label = Some("my linking task"),
      description = Some("some comment"),
      modified = Some(Instant.now)
    )

  override def workspaceProviderId = "inMemory"

  protected override def routes = Some(classOf[test.Routes])

  "Setup project" in {
    createProject(project)
    addProjectPrefixes(project)
    createVariableDataset(project, sourceDataset)
    createVariableDataset(project, targetDataset)
    createVariableDataset(project, outputDataset)
    workspaceProject(project).resources.get(csvResource).writeString(
      """id,label,group
        |1,entry 1,group 1
        |2,entry 2,group 1
        |""".stripMargin)
    workspaceProject(project).resources.get(outputCsvResource).writeString("")
    createCsvFileDataset(project, csvSource1, csvResource)
    createCsvFileDataset(project, csvSource2, csvResource)
    createCsvFileDataset(project, outputCsv, outputCsvResource)
  }

  "Add a linking task" in {
    createLinkingTask(project, task, sourceDataset, targetDataset, outputDataset)
    createLinkingTask(project, csvLinkingTask, csvSource1, csvSource2, outputCsv)
  }

  "Update meta data" in {
    updateMetaData(project, task, metaData)
  }

  "Update linkage rule" in {
    setLinkingRule(project, task,
      <LinkageRule linkType="&lt;http://www.w3.org/2002/07/owl#sameAs&gt;">
        <Aggregate id="combineSimilarities" required="false" weight="1" type="min">
          <Compare id="compareTitles" required="false" weight="1" metric="levenshteinDistance" threshold="0.0" indexing="true">
            <TransformInput id="toLowerCase1" function="lowerCase">
              <Input id="movieTitle1" path="&lt;http://xmlns.com/foaf/0.1/name&gt;"/>
            </TransformInput>
            <TransformInput id="toLowerCase2" function="lowerCase">
              <Input id="movieTitle2" path="&lt;http://www.w3.org/2000/01/rdf-schema#label&gt;"/>
            </TransformInput>
          </Compare>
        </Aggregate>
      </LinkageRule>
    )
    setLinkingRule(project, csvLinkingTask,
      <LinkageRule linkType="&lt;http://www.w3.org/2002/07/owl#sameAs&gt;">
        <Compare id="compareLabels" required="true" weight="1" metric="equality" threshold="0.0" indexing="true">
          <Input id="label1" path="label"/>
          <Input id="label2" path="label"/>
        </Compare>
      </LinkageRule>
    )
  }

  "Check meta data" in {
    // Compare ignoring modified date
    getMetaData(project, task).copy(created = metaData.created, modified = metaData.modified) mustBe metaData
  }

  "Execute with alternative linking rule" in {
    evaluateLinkageRule()
    evaluateLinkageRule(linkLimit = Some(2), expectedLinks = 2)
  }

  "Return evaluated links for the current linking rule" in {
    val jsonBody: JsValue = linkEvaluationResult()
    (jsonBody \ "links").as[JsArray].value must have size 2
    (jsonBody \\ "decision").map(_.as[String]) mustBe Seq("unlabeled", "unlabeled")
    (jsonBody \ "evaluationActivityStats").as[LinkRuleEvaluationStats] mustBe LinkRuleEvaluationStats(2, 2, 2)
  }

  "Return evaluated links for the current linking rule matching the search query" in {
    linkCountMustBe(linkEvaluationResult("simplecsv entry"), 2)
    linkCountMustBe(linkEvaluationResult("simplecsv entry 1"), 1)
  }

  "Changing the decision of a link should be reflected in the result" in {
    // Change one link to positive
    updateReferenceLink("urn:instance:simplecsv#2", "urn:instance:simplecsv#2", "positive")
    val jsonBody: JsValue = linkEvaluationResult()
    (jsonBody \\ "decision").map(_.as[String]).sorted mustBe Seq("positive", "unlabeled")
  }

  "Link filters should be disjunctive" in {
    // Test 1 filter
    (linkEvaluationResult(filters = Some(Seq(
      EvaluationLinkFilterEnum.positiveLinks
    ))) \\ "decision").map(_.as[String]) mustBe Seq("positive")
    // Test 2 filters
    (linkEvaluationResult(filters = Some(Seq(
      EvaluationLinkFilterEnum.positiveLinks,
      EvaluationLinkFilterEnum.undecidedLinks
    ))) \\ "decision").map(_.as[String]).sorted mustBe Seq("positive", "unlabeled")
  }

  "Evaluated reference links can be fetched" in {
    // Add a reference link that is not in the evaluated ones
    updateReferenceLink("urn:instance:simplecsv#1", "urn:instance:simplecsv#2", "negative")
    val evaluatedLinksOnly = linkEvaluationResult()
    val withReferenceLinks = linkEvaluationResult(includeReferenceLinks = Some(true))
    val referenceLinksOnly = linkEvaluationResult(includeReferenceLinks = Some(true), includeEvaluationLinks = Some(false))
    linkCountMustBe(evaluatedLinksOnly, 2)
    linkCountMustBe(withReferenceLinks, 3)
    val links = linkCountMustBe(referenceLinksOnly, 2)
    links.filter(link => link.source.endsWith("1") && link.target.endsWith("2")) must not be empty
  }

  private def linkCountMustBe(resultJson: JsValue, expectedCount: Int): Seq[Link] = {
    implicit val readContext: ReadContext = TestReadContext()
    val links = (resultJson \ "links").as[JsArray].value.toSeq
    links must have size expectedCount
    links.map(link => LinkJsonFormat.read(link))
  }

  private def updateReferenceLink(source: String, target: String, decision: String): Unit = {
    val response = client
      .url(s"$baseUrl/linking/tasks/$project/$csvLinkingTask/referenceLink?source=${URLEncoder.encode(source, "UTF-8")}" +
        s"&target=${URLEncoder.encode(target, "UTF-8")}&linkType=$decision")
      .put("")
    checkResponse(response)
  }

  private def linkEvaluationResult(query: String = "",
                                   filters: Option[Seq[EvaluateCurrentLinkageRuleRequest.EvaluationLinkFilterEnum.Value]] = None,
                                   includeEvaluationLinks: Option[Boolean] = None,
                                   includeReferenceLinks: Option[Boolean] = None): JsValue = {
    workspaceProject(project).task[LinkSpec](csvLinkingTask).activity[EvaluateLinkingActivity].control.startBlocking()
    val request = client.url(s"$baseUrl/linking/tasks/$project/$csvLinkingTask/evaluate?query=$query")
    val response = request.
      post(Json.toJson(EvaluateCurrentLinkageRuleRequest(
        query = Some(query),
        filters = filters,
        includeEvaluationLinks = includeEvaluationLinks,
        includeReferenceLinks = includeReferenceLinks
      )))
    val jsonBody = checkResponse(response).json
    jsonBody
  }

  // Alternative linkage rule returns 4 links, the original rule only returns 2
  private val csvLinkingNrOfLinks = 4
  // Executes the evaluateLinkageRule with alternative linkage rule and checks results
  private def evaluateLinkageRule(linkLimit: Option[Int] = None,
                                  expectedLinks: Int = csvLinkingNrOfLinks): Unit = {
    // Alternative linkage rule
    val inputPath = () => PathInput(path = UntypedPath("group"))
    val alternativeLinkingRule = LinkageRule(Some(
      Comparison(metric = EqualityMetric(), inputs = DPair(inputPath(), inputPath()))
    ))
    // Make request
    implicit val writeContext: WriteContext[JsValue] = TestWriteContext[JsValue]()
    val linkageRuleJson = LinkageRuleJsonFormat.write(alternativeLinkingRule)
    val linkLimitQuery = linkLimit.map(ll => s"?linkLimit=$ll").getOrElse("")
    val request = client.url(s"$baseUrl/linking/tasks/$project/$csvLinkingTask/evaluateLinkageRule" + linkLimitQuery)
    val response = request.
      addHttpHeaders("Content-Type"-> SerializationUtils.APPLICATION_JSON).
      post(linkageRuleJson)
    // Check results
    val json = checkResponse(response).json
    val jsLinks = json.as[JsArray].value
    jsLinks.size mustBe expectedLinks
    val ruleValues = (jsLinks.head \ LinkJsonFormat.RULE_VALUES).toOption
    ruleValues mustBe defined
    (ruleValues.get \ "sourceValue" \ "values").as[JsArray].value.map(_.as[String]) mustBe Seq("group 1")
  }
}
