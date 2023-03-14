package controllers.transform

import controllers.transform.routes.EvaluateTransformApi
import helper.{ApiClient, IntegrationTestTrait}
import org.scalatest.{FlatSpec, Matchers}
import org.silkframework.workspace.SingleProjectWorkspaceProviderTestTrait
import play.api.libs.json.{JsArray, JsValue, Json}
import play.api.routing.Router

class EvaluateTransformApiTest extends FlatSpec with Matchers with SingleProjectWorkspaceProviderTestTrait with IntegrationTestTrait with ActiveLearningApiClient {

  override def projectPathInClasspath: String = "controllers/transform/evaluateTransformTest.zip"

  override def workspaceProviderId: String = "inMemory"

  override def routes: Option[Class[_ <: Router]] = Some(classOf[test.Routes])

  behavior of "EvaluateTransformApi"

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

}

trait ActiveLearningApiClient extends ApiClient {

  def evaluate(projectId: String, taskId: String, ruleId: String, rule: JsValue, limit: Int = 3): JsValue = {
    val request = createRequest(EvaluateTransformApi.evaluateRule(projectId, taskId, ruleId, limit))
    val response = checkResponse(request.post(rule))
    response.json
  }

}
