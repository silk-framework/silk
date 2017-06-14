package controllers.transform

import play.api.libs.json.{JsArray, JsString, Json}
import play.api.libs.ws.WS

import scala.concurrent.duration._
import scala.concurrent.Await

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

  "Set root mapping parameters: URI pattern and type" in {
    val json = jsonPutRequest(s"$baseUrl/transform/tasks/$project/$task/rule/root") {
      """
        {
          "rules": {
            "uriRule": {
              "type": "uri",
              "pattern": "http://example.org/{PersonID}"
            },
            "typeRules": [
              {
                "type": "type",
                "id": "explicitlyDefinedId",
                "typeUri": "target:Person"
              }
            ]
          }
        }
      """
    }

    // Do some spot checks
    (json \ "rules" \ "uriRule" \ "pattern").as[JsString].value mustBe "http://example.org/{PersonID}"
    (json \ "rules" \ "propertyRules").as[JsArray].value mustBe Array.empty
  }

  "Append new direct mapping rule to root" in {
    jsonPostRequest(s"$baseUrl/transform/tasks/$project/$task/rule/root/rules") {
      """
        {
          "id": "directRule",
          "type": "direct",
          "sourcePath": "/source:name",
          "mappingTarget": {
            "uri": "target:name",
            "valueType": {
              "nodeType": "StringValueType"
            }
          },
          "metaData" : {
            "label" : "My direct rule",
            "description" : "Description of direct rule"
          }
        }
      """
    }
  }

  "Append new object mapping rule to root" in {
    jsonPostRequest(s"$baseUrl/transform/tasks/$project/$task/rule/root/rules") {
      """
        {
          "id": "objectRule",
          "type": "object",
          "sourcePath": "source:address",
          "mappingTarget": {
            "uri": "target:address",
            "valueType": {
              "nodeType": "UriValueType"
            }
          },
          "rules": {
            "uriRule": null,
            "typeRules": [
            ],
            "propertyRules": [
            ]
          }
        }
      """
    }
  }

  "Retrieve full mapping rule tree" in {
    jsonGetRequest(s"$baseUrl/transform/tasks/$project/$task/rules") mustMatchJson {
      """
        {
          "type" : "root",
          "id" : "root",
          "rules" : {
            "uriRule" : {
              "type" : "uri",
              "id" : "uri",
              "pattern" : "http://example.org/{PersonID}",
              "metaData" : {
                "label" : "",
                "description" : ""
              }
            },
            "typeRules" : [ {
              "type" : "type",
              "id" : "explicitlyDefinedId",
              "typeUri" : "target:Person",
              "metaData" : {
                "label" : "",
                "description" : ""
              }
            } ],
            "propertyRules" : [ {
              "type" : "direct",
              "id" : "directRule",
              "sourcePath" : "/source:name",
              "mappingTarget" : {
                "uri" : "target:name",
                "valueType" : {
                  "nodeType" : "StringValueType"
                },
 |              "isBackwardProperty" : false
              },
              "metaData" : {
                "label" : "My direct rule",
                "description" : "Description of direct rule"
              }
            }, {
              "type" : "object",
              "id" : "objectRule",
              "sourcePath" : "/source:address",
              "mappingTarget" : {
                "uri" : "target:address",
                "valueType" : {
                  "nodeType" : "UriValueType"
                },
                "isBackwardProperty" : false
              },
              "rules" : {
                "uriRule" : null,
                "typeRules" : [ ],
                "propertyRules" : [ ]
              },
              "metaData" : {
                "label" : "",
                "description" : ""
              }
            } ]
          },
          "metaData" : {
            "label" : "",
            "description" : ""
          }
        }

      """
    }
  }

  "Retrieve a single mapping rule" in {
    jsonGetRequest(s"$baseUrl/transform/tasks/$project/$task/rule/objectRule") mustMatchJson {
      """
        {
          "type" : "object",
          "id" : "objectRule",
          "sourcePath" : "/source:address",
          "mappingTarget" : {
            "uri" : "target:address",
            "valueType" : {
              "nodeType" : "UriValueType"
            },
            "isBackwardProperty" : false
          },
          "rules" : {
            "uriRule" : null,
            "typeRules" : [ ],
            "propertyRules" : [ ]
          },
          "metaData" : {
            "label" : "",
            "description" : ""
          }
        }
      """
    }
  }

  "Reorder the child rules" in {
    jsonPostRequest(s"$baseUrl/transform/tasks/$project/$task/rule/root/rules/reorder") {
      """
        [
          "objectRule",
          "directRule"
        ]
      """
    }

    // Check if the rules have been reordered correctly
    val fullTree = jsonGetRequest(s"$baseUrl/transform/tasks/$project/$task/rules")
    val order = (fullTree \ "rules" \ "propertyRules").as[JsArray].value.map(r => (r \ "id").as[JsString].value).toSeq
    order mustBe Seq("objectRule", "directRule")
  }

  "Delete mapping rule" in {
    val request = WS.url(s"$baseUrl/transform/tasks/$project/$task/rule/objectRule")
    val response = request.delete()
    checkResponse(response)
  }

  "Return 404 if a requested rule does not exist" in {
    var request = WS.url(s"$baseUrl/transform/tasks/$project/$task/rule/objectRule")
    request = request.withHeaders("Accept" -> "application/json")
    val response = Await.result(request.get(), 100.seconds)
    response.status mustBe 404
  }

  "Return 404 if submitted mapping parameters are invalid" in {
    var request = WS.url(s"$baseUrl/transform/tasks/$project/$task/rule/root")
    request = request.withHeaders("Accept" -> "application/json")

    val json =
      """
        {
          "rules": {
            "typeRules": [
              {
                "type": "type",
                "id": "explicitlyDefinedId",
                "typeUri": "invalidPrefix:Person"
              }
            ]
          }
        }
      """

    val response = Await.result(request.put(Json.parse(json)), 100.seconds)
    response.status mustBe 400
  }

  "Return 404 if an invalid rule should be appended" in {
    var request = WS.url(s"$baseUrl/transform/tasks/$project/$task/rule/root/rules")
    request = request.withHeaders("Accept" -> "application/json")

    val json =
      """
        {
          "id": "invalidRule",
          "type": "direct",
          "sourcePath": "/invalidPrefix:name",
          "mappingTarget": {
            "uri": "target:name",
            "valueType": {
              "nodeType": "StringValueType"
            }
          }
        }
      """

    val response = Await.result(request.post(Json.parse(json)), 100.seconds)
    response.status mustBe 400
  }

  "Return 404 if an invalid rule json is provided" in {
    var request = WS.url(s"$baseUrl/transform/tasks/$project/$task/rule/root/rules")
    request = request.withHeaders("Accept" -> "application/json")

    val json =
      """
        {
          "id": "invalidRule",
          "type": "direct",
          "sourcePath": "/invalidPrefix:name",
          "mappingTarget": {
            "uri": [ "there should not be an array here"],
            "valueType": {
              "nodeType": "StringValueType"
            }
          }
        }
      """

    val response = Await.result(request.post(Json.parse(json)), 100.seconds)
    response.status mustBe 400
  }

}
