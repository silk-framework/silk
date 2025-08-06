package controllers.transform

import controllers.transform.routes.EvaluateTransformApi
import helper.{ApiClient, IntegrationTestTrait}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.workspace.SingleProjectWorkspaceProviderTestTrait
import play.api.libs.json.{JsArray, JsValue, Json}
import play.api.routing.Router

class EvaluateTransformApiTest extends AnyFlatSpec with Matchers with SingleProjectWorkspaceProviderTestTrait
  with IntegrationTestTrait with ActiveLearningApiClient {

  override def projectPathInClasspath: String = "controllers/transform/evaluateTransformTest.zip"

  override def workspaceProviderId: String = "inMemoryWorkspaceProvider"

  override def routes: Option[Class[_ <: Router]] = Some(classOf[test.Routes])

  behavior of "EvaluateTransformApi"

  private val complexTransformId = "Complextransform_f100e44e303cc4fb"

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
}

trait ActiveLearningApiClient extends ApiClient {

  def evaluate(projectId: String, taskId: String, ruleId: String, rule: JsValue, limit: Int = 3): JsValue = {
    val request = createRequest(EvaluateTransformApi.evaluateRule(projectId, taskId, ruleId, limit))
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
