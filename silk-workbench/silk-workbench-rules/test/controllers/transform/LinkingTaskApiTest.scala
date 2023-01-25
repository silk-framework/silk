package controllers.transform

import controllers.linking.evaluation.{EvaluateCurrentLinkageRuleRequest, LinkRuleEvaluationStats}

import java.time.Instant
import controllers.util.SerializationUtils
import helper.IntegrationTestTrait
import org.scalatestplus.play.PlaySpec
import org.silkframework.config.MetaData
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.rule.{LinkSpec, LinkageRule}
import org.silkframework.rule.input.PathInput
import org.silkframework.rule.plugins.distance.equality.EqualityMetric
import org.silkframework.rule.similarity.Comparison
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import org.silkframework.serialization.json.JsonSerializers.LinkageRuleJsonFormat
import org.silkframework.serialization.json.LinkingSerializers.LinkJsonFormat
import org.silkframework.util.DPair
import org.silkframework.workspace.activity.linking.EvaluateLinkingActivity
import play.api.libs.json.{JsArray, JsValue, Json}

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
    (jsonBody \ "stats").as[LinkRuleEvaluationStats] mustBe LinkRuleEvaluationStats(2, 2, 2)
  }

  "Return evaluated links for the current linking rule matching the search query" in {
    (linkEvaluationResult("simplecsv entry") \ "links").as[JsArray].value must have size 2
    (linkEvaluationResult("simplecsv entry 1") \ "links").as[JsArray].value must have size 1
  }

  "Changing the decision of a link should be reflected in the result" in {
    // Change one link to positive
    val response = client
      .url(s"$baseUrl/linking/tasks/$project/$csvLinkingTask/referenceLink?source=urn%3Ainstance%3Asimplecsv%232&target=urn%3Ainstance%3Asimplecsv%232&linkType=positive")
      .put("")
    checkResponse(response)
    val jsonBody: JsValue = linkEvaluationResult()
    (jsonBody \\ "decision").map(_.as[String]).sorted mustBe Seq("positive", "unlabeled")
  }

  private def linkEvaluationResult(query: String = ""): JsValue = {
    workspaceProject(project).task[LinkSpec](csvLinkingTask).activity[EvaluateLinkingActivity].control.startBlocking()
    val request = client.url(s"$baseUrl/linking/tasks/$project/$csvLinkingTask/evaluate?query=$query")
    val response = request.
      post(Json.toJson(EvaluateCurrentLinkageRuleRequest(query = Some(query))))
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
    implicit val writeContext: WriteContext[JsValue] = WriteContext[JsValue]()
    val linkageRuleJson = LinkageRuleJsonFormat.write(alternativeLinkingRule)
    val linkLimitQuery = linkLimit.map(ll => s"?linkLimit=$ll").getOrElse("")
    val request = client.url(s"$baseUrl/linking/tasks/$project/$csvLinkingTask/evaluateLinkageRule" + linkLimitQuery)
    val response = request.
        withHttpHeaders("Content-Type"-> SerializationUtils.APPLICATION_JSON).
        post(linkageRuleJson)
    // Check results
    val json = checkResponse(response).json
    implicit val readContext: ReadContext = ReadContext()
    val jsLinks = json.as[JsArray].value
    jsLinks.size mustBe expectedLinks
    val ruleValues = (jsLinks.head \ LinkJsonFormat.RULE_VALUES).toOption
    ruleValues mustBe defined
    (ruleValues.get \ "sourceValue" \ "values").as[JsArray].value.map(_.as[String]) mustBe Seq("group 1")
  }
}
