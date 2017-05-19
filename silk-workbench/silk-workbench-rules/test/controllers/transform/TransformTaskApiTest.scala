package controllers.transform

import play.api.libs.json.{JsArray, JsString, JsValue, Json}
import play.api.libs.ws.WS

class TransformTaskApiTest extends TransformTaskApiTestBase {

  def printResponses = true

  "Setup project" in {
    createProject(project)
    addProjectPrefixes(project)
    createVariableDataset(project, "dataset")
  }

  "Add a new empty transform task" in {
    val request = WS.url(s"$baseUrl/transform/tasks/$project/$task")
    val response = request.put(Map("source" -> Seq("dataset")))
    checkResponse(response)
  }

  "Append new direct mapping rule to root" in {
    val request = WS.url(s"$baseUrl/transform/tasks/$project/$task/rule/root")
    val response = request.post(Json.parse(
      """
        | {
        |   "id": "directRule",
        |   "type": "direct",
        |   "sourcePath": "/source:name",
        |   "mappingTarget": {
        |     "uri": "target:name",
        |     "valueType": {
        |       "nodeType": "StringValueType"
        |     }
        |   }
        | }
      """.stripMargin
    ))
    checkResponse(response)
  }

  "Append new object mapping rule to root" in {
    val request = WS.url(s"$baseUrl/transform/tasks/$project/$task/rule/root")
    val response = request.post(Json.parse(
      """
        | {
        |   "id": "objectRule",
        |   "type": "hierarchical",
        |   "sourcePath": "source:address",
        |   "mappingTarget": {
        |     "uri": "target:address",
        |     "valueType": {
        |       "nodeType": "UriValueType"
        |     }
        |   },
        |   "rules": {
        |     "uriRule": null,
        |     "typeRules": [
        |     ],
        |     "propertyRules": [
        |     ]
        |   }
        | }
      """.stripMargin
    ))
    checkResponse(response)
  }

  "Retrieve full mapping rule tree" in {
    jsonGetRequest(s"$baseUrl/transform/tasks/$project/$task/rules")
  }

  "Reorder the child rules" in {
    val request = WS.url(s"$baseUrl/transform/tasks/$project/$task/rule/root/reorder")
    val response = request.post(Json.parse(
      """
        | [
        |   "objectRule",
        |   "directRule"
        | ]
      """.stripMargin
    ))
    checkResponse(response)

    // Check if the rules have been reordered correctly
    val fullTree = jsonGetRequest(s"$baseUrl/transform/tasks/$project/$task/rules")
    val order = (fullTree \ "rules" \ "propertyRules").as[JsArray].value.map(r => (r \ "id").as[JsString].value).toSeq
    order mustBe Seq("objectRule", "directRule")
  }

}
