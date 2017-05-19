package controllers.transform

import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WS

class TransformTaskApiTest extends TransformTaskApiTestBase {

  def printResponses = false

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
        |   "id": "newRuleId",
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
        |   "id": "newRuleId2",
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



}
