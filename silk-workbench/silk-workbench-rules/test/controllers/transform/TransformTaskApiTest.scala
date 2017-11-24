package controllers.transform

import play.api.libs.json.{JsArray, JsString, Json}
import play.api.libs.ws.WS

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

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
          "type": "direct",
          "id": "directRule",
          "sourcePath": "/source:name",
          "mappingTarget": {
            "uri": "target:name",
            "valueType": {
              "nodeType": "StringValueType"
            }
          },
          "metadata" : {
            "label" : "direct rule label",
            "description" : "direct rule description"
          }
        }
      """
    }
  }

  "Update meta data of direct mapping rule" in {
    jsonPutRequest(s"$baseUrl/transform/tasks/$project/$task/rule/directRule") {
      """
        {
          "metadata" : {
            "label" : "updated direct rule label",
            "description" : "updated direct rule description"
          }
        }
      """
    }
  }

  "Append new object mapping rule to root" in {
    jsonPostRequest(s"$baseUrl/transform/tasks/$project/$task/rule/root/rules") {
      """
        {
          "type": "object",
          "id": "objectRule",
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
              "metadata" : {
                "label" : "",
                "description" : ""
              }
            },
            "typeRules" : [ {
              "type" : "type",
              "id" : "explicitlyDefinedId",
              "typeUri" : "target:Person",
              "metadata" : {
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
                "isBackwardProperty" : false,
                "isAttribute": false
              },
              "metadata" : {
                "label" : "updated direct rule label",
                "description" : "updated direct rule description"
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
                "isBackwardProperty" : false,
                "isAttribute": false
              },
              "rules" : {
                "uriRule" : null,
                "typeRules" : [ ],
                "propertyRules" : [ ]
              },
              "metadata" : {
                "label" : "",
                "description" : ""
              }
            } ]
          },
          "metadata" : {
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
            "isBackwardProperty" : false,
            "isAttribute": false
          },
          "rules" : {
            "uriRule" : null,
            "typeRules" : [ ],
            "propertyRules" : [ ]
          },
          "metadata" : {
            "label" : "",
            "description" : ""
          }
        }
      """
    }
  }

  "Append another direct mapping rule to root" in {
    jsonPostRequest(s"$baseUrl/transform/tasks/$project/$task/rule/root/rules") {
      """
        {
          "type": "direct",
          "id": "directRule2",
          "sourcePath": "/source:lastName",
          "mappingTarget": {
            "uri": "target:lastName",
            "valueType": {
              "nodeType": "StringValueType"
            }
          },
          "metadata" : {
            "label" : "direct rule label",
            "description" : "direct rule description"
          }
        }
      """
    }
  }

  "Reorder the child rules" in {
    jsonPostRequest(s"$baseUrl/transform/tasks/$project/$task/rule/root/rules/reorder") {
      """
        [
          "directRule2",
          "objectRule",
          "directRule"
        ]
      """
    }

    // Check if the rules have been reordered correctly
    retrieveRuleOrder() mustBe Seq("directRule2", "objectRule", "directRule")
  }

  "Update direct mapping rule" in {
    jsonPutRequest(s"$baseUrl/transform/tasks/$project/$task/rule/directRule") {
      """
        {
          "sourcePath": "/source:firstName",
          "mappingTarget": {
            "uri": "target:firstName",
            "isAttribute": true
          }
        }
      """
    }

    // Check if rule has been update correctly
    jsonGetRequest(s"$baseUrl/transform/tasks/$project/$task/rule/directRule") mustMatchJson {
      """
        {
          "type": "direct",
          "id": "directRule",
          "sourcePath": "/source:firstName",
          "mappingTarget": {
            "uri": "target:firstName",
            "valueType": {
              "nodeType": "StringValueType"
            },
            "isBackwardProperty": false,
            "isAttribute": true
          },
          "metadata" : {
            "label" : "updated direct rule label",
            "description" : "updated direct rule description"
          }
        }
      """
    }

    // Make sure that the position of the updated rule did not change
    retrieveRuleOrder() mustBe Seq("directRule2", "objectRule", "directRule")
  }

  "Set complex URI pattern" in {
    jsonPutRequest(s"$baseUrl/transform/tasks/$project/$task/rule/objectRule") {
      """
        {
          "rules": {
            "uriRule": {
              "id": "complexUriRule",
              "type": "complexUri",
              "operator": {
                "type": "transformInput",
                "id": "constant",
                "function": "constant",
                "inputs": [],
                "parameters": {
                  "value": "http://example.org/constantUri"
                }
              }
            }
          }
        }
      """
    }

    // Do some spot checks
    val json = jsonGetRequest(s"$baseUrl/transform/tasks/$project/$task/rule/objectRule")
    val uriRule = json \ "rules" \ "uriRule"
    (uriRule \ "type").get mustBe JsString("complexUri")
    (uriRule \ "operator" \ "function").get mustBe JsString("constant")
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

  "Return 400 if submitted mapping parameters are invalid" in {
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

  "Return 400 if an invalid rule should be appended" in {
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

  "Return 400 if an invalid rule json is provided" in {
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

  "Append multiple new direct mapping rules without ID at the same time" in {
    val resultsFutures = for(i <- 1 to 10) yield {
      Future(jsonPostRequest(s"$baseUrl/transform/tasks/$project/$task/rule/root/rules") {
        s"""
        {
          "type": "direct",
          "sourcePath": "/source:name",
          "mappingTarget": {
            "uri": "target:name$i",
            "valueType": {
              "nodeType": "StringValueType"
            }
          },
          "metadata" : {
            "label" : "direct rule label $i",
            "description" : "direct rule description"
          }
        }
      """
      })
    }
    val seqFuture = Future.sequence(resultsFutures)
    val jsons = Await.result(seqFuture, 10.seconds)
    jsons.map(json => (json \ "id").as[String]).distinct.size mustBe 10
  }
}
