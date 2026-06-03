package controllers.transform

import controllers.transform.routes.EvaluateTransformApi
import helper.{ApiClient, IntegrationTestTrait}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.rule.input.{InputPortInput, PathInput, RuleBlockBinding, RuleBlockInput, TransformInput}
import org.silkframework.rule.plugins.transformer.normalize.LowerCaseTransformer
import org.silkframework.rule.{ComplexMapping, RuleBlockModel, RuleBlockPort, RuleBlockSpec, TransformRule}
import org.silkframework.runtime.serialization.WriteContext
import org.silkframework.serialization.json.JsonSerialization
import org.silkframework.serialization.json.JsonSerializers.TransformRuleJsonFormat
import org.silkframework.workspace.SingleProjectWorkspaceProviderTestTrait
import play.api.libs.json.{JsArray, JsValue, Json}
import play.api.routing.Router
import org.silkframework.util.Identifier

class EvaluateTransformApiTest extends AnyFlatSpec with Matchers with SingleProjectWorkspaceProviderTestTrait
  with IntegrationTestTrait with ActiveLearningApiClient {

  override def projectPathInClasspath: String = "controllers/transform/evaluateTransformTest.zip"

  override def workspaceProviderId: String = "inMemoryWorkspaceProvider"

  override def routes: Option[Class[_ <: Router]] = Some(classOf[test.Routes])

  behavior of "EvaluateTransformApi"

  private val complexTransformId = "Complextransform_f100e44e303cc4fb"
  private val ruleBlockDatasetId = "ruleBlockSource"
  private val ruleBlockOutputId = "ruleBlockOutput"
  private val ruleBlockTransformId = "transformWithRuleBlock"
  private val ruleBlockTaskId = "normalizeName"
  private val ruleBlockResource = "ruleBlockTransform.csv"

  it should "evaluate rules" in {
    val json = Json.parse(
      """
        |{
        |  "type" : "complex",
        |  "id" : "label",
        |  "operator" : {
        |    "type" : "transformInput",
        |    "id" : "lowerCase",
        |    "function" : "lowerCase",
        |    "inputs" : [ {
        |      "type" : "pathInput",
        |      "id" : "label",
        |      "path" : "loanState"
        |    } ],
        |    "parameters" : { }
        |  },
        |  "mappingTarget" : {
        |    "uri" : "loanState",
        |    "valueType" : {
        |      "nodeType" : "StringValueType"
        |    },
        |    "isBackwardProperty" : false,
        |    "isAttribute" : false
        |  }
        |}
        |""".stripMargin)
    val results = evaluate(projectId, "transform_b45e924cf97d1208", "root", json).as[JsArray]
    results.value.size shouldBe 3
    val firstResult = results.value.head

    (firstResult \ "values").as[Seq[String]] shouldBe Seq("arizona")
    ((firstResult \ "children").head \ "values").as[Seq[String]] shouldBe Seq("Arizona")
  }

  it should "evaluate an existing root rule" in {
    val (rules, entities) = evaluated(projectId, complexTransformId, "root")
    entities should have size 3
    rules should have size 5
    val firstEntityValues = (entities.head \ "values").as[JsArray].value
    firstEntityValues should have size 5
    // First buildUri value is of the root URI rule, other 2 are from object mapping rules
    firstEntityValues map (js => (js \ "operatorId").as[String]) shouldBe Seq("buildUri", "label", "buildUri", "vol", "buildUri")
    (entities.head \ "uris").as[Seq[String]] shouldBe (firstEntityValues.head \ "values").as[Seq[String]]
  }

  it should "allow filtering out entities without URIs" in {
    val (_, entitiesWithoutFilter) = evaluated(projectId, complexTransformId, ruleId = "childRare", limit = 50)
    entitiesWithoutFilter should have size 50
    val (_, entitiesWithFilter) = evaluated(projectId, complexTransformId, ruleId = "childRare", limit = 50, showOnlyEntitiesWithUris = true)
    entitiesWithFilter should have size 1
  }

  it should "return the evaluation for default URI rules" in {
    val (rules, entities) = evaluated(projectId, complexTransformId, ruleId = "child", showOnlyEntitiesWithUris = true)
    rules should have size 3
    val firstEntityValues = (entities.head \ "values").as[JsArray].value
    firstEntityValues map (js => (js \ "operatorId").as[String]) shouldBe Seq("buildUri", "childLabel", "zip")
    (entities.head \ "uris").as[Seq[String]] shouldBe (firstEntityValues.head \ "values").as[Seq[String]]
  }

  it should "include reusable rule block snapshots keyed by task ID" in {
    workspaceProject(projectId).resources.get(ruleBlockResource).writeString(
      """id,name
        |1,ALICE
        |""".stripMargin
    )
    createCsvFileDataset(projectId, ruleBlockDatasetId, ruleBlockResource)
    createVariableDataset(projectId, ruleBlockOutputId)
    createTransformTask(projectId, ruleBlockTransformId, ruleBlockDatasetId, ruleBlockOutputId)
    workspaceProject(projectId).addTask(
      ruleBlockTaskId,
      RuleBlockSpec(
        RuleBlockModel(
          ports = IndexedSeq(
            RuleBlockPort(id = Identifier("nameInput"), label = "Name", displayOrder = 1)
          ),
          operator = Some(
            TransformInput(
              id = Identifier("normalizeInput"),
              transformer = LowerCaseTransformer(),
              inputs = IndexedSeq(
                InputPortInput(id = Identifier("namePortInput"), portId = Identifier("nameInput"))
              )
            )
          )
        )
      )
    )

    val postedRule = ComplexMapping(
      id = Identifier("ruleBlockMapping"),
      operator = RuleBlockInput(
        id = Identifier("ruleBlockUsage"),
        ruleBlockId = Identifier(ruleBlockTaskId),
        bindings = IndexedSeq(
          RuleBlockBinding(
            portId = Identifier("nameInput"),
            input = PathInput(id = Identifier("namePath"), path = UntypedPath("name"))
          )
        )
      )
    )
    implicit val writeContext: WriteContext[JsValue] = WriteContext.fromProject[JsValue](workspaceProject(projectId))
    val response = evaluateResponse(
      projectId,
      ruleBlockTransformId,
      "root",
      JsonSerialization.toJson[TransformRule](postedRule),
      includeRuleBlockInspection = true
    )

    val snapshots = (response \ "ruleBlockInspection" \ "snapshots").as[Map[String, JsValue]]
    snapshots.keySet shouldBe Set(ruleBlockTaskId)
    (snapshots(ruleBlockTaskId) \ "operatorTree" \ "id").as[String] shouldBe "normalizeInput"

    val evaluatedValues = (response \ "evaluatedValues").as[JsArray].value
    evaluatedValues should have size 1
    val internalOperatorId = (((evaluatedValues.head \ "children").as[JsArray].value.head) \ "operatorId").as[String]
    internalOperatorId shouldBe (snapshots(ruleBlockTaskId) \ "operatorTree" \ "id").as[String]
  }
}

trait ActiveLearningApiClient extends ApiClient {

  def evaluate(projectId: String, taskId: String, ruleId: String, rule: JsValue, limit: Int = 3): JsValue = {
    evaluateResponse(projectId, taskId, ruleId, rule, limit)
  }

  def evaluateResponse(projectId: String,
                       taskId: String,
                       ruleId: String,
                       rule: JsValue,
                       limit: Int = 3,
                       includeRuleBlockInspection: Boolean = false): JsValue = {
    val request = createRequest(EvaluateTransformApi.evaluateRule(projectId, taskId, ruleId, limit, includeRuleBlockInspection))
    val response = checkResponse(request.post(rule))
    response.json
  }

  def evaluated(projectId: String, taskId: String, ruleId: String, limit: Int = 3, showOnlyEntitiesWithUris: Boolean = false): (Seq[JsValue], Seq[JsValue]) = {
    val request = createRequest(EvaluateTransformApi.evaluateSpecificRule(projectId, taskId, ruleId, limit, showOnlyEntitiesWithUris))
    val response = checkResponse(request.get())
    val results = response.json
    val rules = (results \ "rules").as[JsArray].value.toSeq
    val entities = (results \ "evaluatedEntities").as[JsArray].value.toSeq
    (rules, entities)
  }
}
