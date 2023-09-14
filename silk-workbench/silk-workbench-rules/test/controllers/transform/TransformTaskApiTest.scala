package controllers.transform

import org.silkframework.entity.ValueType
import org.silkframework.rule.*
import org.silkframework.serialization.json.JsonSerializers.{DATA, PARAMETERS}
import org.silkframework.workspace.ProjectTask
import play.api.libs.json.{JsArray, JsString, JsValue, Json}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.*
import scala.concurrent.{Await, Future}
import scala.xml.XML

class TransformTaskApiTest extends TransformTaskApiTestBase {

  def printResponses = false

  private val OBJECT_RULE_ID = "objectRule"
  private val ROOT_RULE_ID = "root"

  "Setup project" in {
    createProject(project)
    addProjectPrefixes(project)
    createVariableDataset(project, "dataset")
  }

  "Add a new empty transform task" in {
    val request = client.url(s"$baseUrl/transform/tasks/$project/$task")
    val response = request.put(Map("source" -> Seq("dataset")))
    checkResponse(response)
  }

  "Check that we can GET the transform task as JSON" in {
    val request = client.url(s"$baseUrl/transform/tasks/$project/$task").
      addHttpHeaders("ACCEPT" -> "application/json")
    val response = Json.parse(checkResponse(request.get()).body)
    // No label should be generated for this transform
    (response \ "metadata" \ "label").isDefined mustBe false
    // An id and a label has been generated for the root mapping
    (response \ DATA \ PARAMETERS \ "mappingRule" \ "id").as[String] mustBe ROOT_RULE_ID
    (response \ DATA \ PARAMETERS \ "mappingRule" \ "metadata" \ "label").isDefined mustBe false
  }

  "Check that we can GET the transform task as XML" in {
    val request = client.url(s"$baseUrl/transform/tasks/$project/$task").
      addHttpHeaders("ACCEPT" -> "application/xml")
    val response = request.get()
    (XML.loadString(checkResponse(response).body) \ "RootMappingRule" \ "@id").toString mustBe ROOT_RULE_ID
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
    (json \ "rules" \ "uriRule" \ "pattern").as[String] mustBe "http://example.org/{PersonID}"
    (json \ "rules" \ "propertyRules").as[JsArray].value mustBe Array.empty[JsValue]
  }

  "Append new direct mapping rule to root" in {
    val json = jsonPostRequest(s"$baseUrl/transform/tasks/$project/$task/rule/root/rules") {
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
            "description" : "direct rule description"
          }
        }
      """
    }
    (json \ "metadata" \ "label").isDefined mustBe false
    (json \ "metadata" \ "description").as[String] mustBe "direct rule description"
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
    val json = jsonPostRequest(s"$baseUrl/transform/tasks/$project/$task/rule/root/rules") {
      s"""
        {
          "type": "object",
          "id": "$OBJECT_RULE_ID",
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
    (json \ "metadata" \ "label").isDefined mustBe false
    (json \ "id").as[String] mustBe OBJECT_RULE_ID
  }

  "Retrieve full mapping rule tree" in {
    jsonGetRequest(s"$baseUrl/transform/tasks/$project/$task/rules") mustMatchJson {
      s"""
        {
 |    "type": "root",
 |    "id": "root",
 |    "rules": {
 |        "uriRule": {
 |            "type": "uri",
 |            "id": "uri",
 |            "pattern": "http://example.org/{PersonID}",
 |            "metadata": {
 |            }
 |        },
 |        "typeRules": [
 |            {
 |                "type": "type",
 |                "id": "explicitlyDefinedId",
 |                "typeUri": "target:Person",
 |                "metadata": {
 |                }
 |            }
 |        ],
 |        "propertyRules": [
 |            {
 |                "type": "direct",
 |                "id": "directRule",
 |                "mappingTarget": {
 |                    "isAttribute": false,
 |                    "isBackwardProperty": false,
 |                    "uri": "target:name",
 |                    "valueType": {
 |                        "nodeType": "StringValueType"
 |                    }
 |                },
 |                "sourcePath": "source:name",
 |                "metadata": {
 |                    "description": "updated direct rule description",
 |                    "label": "updated direct rule label"
 |                }
 |            },
 |            {
 |                "type": "object",
 |                "id": "$OBJECT_RULE_ID",
 |                "mappingTarget": {
 |                    "isAttribute": false,
 |                    "isBackwardProperty": false,
 |                    "uri": "target:address",
 |                    "valueType": {
 |                        "nodeType": "UriValueType"
 |                    }
 |                },
 |                "rules": {
 |                    "propertyRules": [],
 |                    "typeRules": [],
 |                    "uriRule": null
 |                },
 |                "sourcePath": "source:address",
 |                "metadata": {
 |                }
 |            }
 |        ]
 |    },
 |    "mappingTarget": {
 |        "uri":"",
 |        "valueType":{"nodeType":"UriValueType"},
 |        "isBackwardProperty":false,
 |        "isAttribute":false
 |    },
 |    "metadata": {
 |    }
 |}
      """.stripMargin

    }
  }

  "Retrieve a single mapping rule" in {
    jsonGetRequest(s"$baseUrl/transform/tasks/$project/$task/rule/objectRule") mustMatchJson {
      s"""
        {
          "type" : "object",
          "id" : "$OBJECT_RULE_ID",
          "sourcePath" : "source:address",
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
      s"""
        [
          "directRule2",
          "$OBJECT_RULE_ID",
          "directRule"
        ]
      """
    }

    // Check if the rules have been reordered correctly
    retrieveRuleOrder() mustBe Seq("directRule2", OBJECT_RULE_ID, "directRule")
  }

  val insertedAfterRuleId = "insertedAfter"

  "Insert new mapping rule after the second rule" in {
    jsonPostRequest(s"$baseUrl/transform/tasks/$project/$task/rule/root/rules?afterRuleId=objectRule") {
      s"""
        {
          "type": "complex",
          "id": "$insertedAfterRuleId",
          "sourcePath": "/source:prop23",
          "operator": {
              "function": "lowerCase",
              "id": "directRule",
              "inputs": [
                  {
                      "id": "number",
                      "path": "sourceProp",
                      "type": "pathInput"
                  }
              ],
              "parameters": {},
              "type": "transformInput"
          },
          "sourcePaths": [
              "sourceProp"
          ],
          "mappingTarget": {
            "uri": "target:prop23",
            "valueType": {
              "nodeType": "StringValueType"
            }
          }
        }
      """
    }
    retrieveRuleOrder() mustBe Seq("directRule2", OBJECT_RULE_ID, insertedAfterRuleId, "directRule")
  }

  "Update direct mapping rule" in {
    jsonPutRequest(s"$baseUrl/transform/tasks/$project/$task/rule/directRule") {
      """
        {
          "sourcePath": "source:firstName",
          "mappingTarget": {
            "uri": "target:firstName",
            "isAttribute": true
          }
        }
      """
    }

    // Check if rule has been updated correctly
    jsonGetRequest(s"$baseUrl/transform/tasks/$project/$task/rule/directRule") mustMatchJson {
      """
        {
          "type": "direct",
          "id": "directRule",
          "sourcePath": "source:firstName",
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
    retrieveRuleOrder() mustBe Seq("directRule2", OBJECT_RULE_ID, insertedAfterRuleId, "directRule")
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

  "Return 400 if submitted mapping parameters are invalid" in {
    var request = client.url(s"$baseUrl/transform/tasks/$project/$task/rule/root")
    request = request.addHttpHeaders("Accept" -> "application/json")

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
    var request = client.url(s"$baseUrl/transform/tasks/$project/$task/rule/root/rules")
    request = request.addHttpHeaders("Accept" -> "application/json")

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
    var request = client.url(s"$baseUrl/transform/tasks/$project/$task/rule/root/rules")
    request = request.addHttpHeaders("Accept" -> "application/json")

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

  private def transformTask: ProjectTask[TransformSpec] = workspaceProject(project).task[TransformSpec](task)

  "Copy an existing mapping rule and put it next to the existing one" in {
    postRequest(s"$baseUrl/transform/tasks/$project/$task/rule/root/rules/copyFrom?" +
        s"sourceProject=$project&sourceTask=$task&sourceRule=$insertedAfterRuleId&afterRuleId=$insertedAfterRuleId")
    retrieveRuleOrder() mustBe Seq("directRule2", OBJECT_RULE_ID, insertedAfterRuleId, insertedAfterRuleId + "1", "directRule")
    val originalTransformRule = rootPropertyRule(insertedAfterRuleId)
    val clonedTransformRule = rootPropertyRule(insertedAfterRuleId + "1")
    val originalInput = as[ComplexMapping](originalTransformRule).operator
    val clonedInput = as[ComplexMapping](clonedTransformRule).operator
    originalInput mustBe clonedInput
  }

  "Copy an existing object rule and put it to the end of the list" in {
    postRequest(s"$baseUrl/transform/tasks/$project/$task/rule/root/rules/copyFrom?" +
        s"sourceProject=$project&sourceTask=$task&sourceRule=$OBJECT_RULE_ID")
    retrieveRuleOrder() mustBe Seq("directRule2", OBJECT_RULE_ID, insertedAfterRuleId, insertedAfterRuleId + "1",
      "directRule", OBJECT_RULE_ID + "1")
    val originalTransformRule = rootPropertyRule(OBJECT_RULE_ID)
    val clonedTransformRule = rootPropertyRule(OBJECT_RULE_ID + "1")
    val originalInput = as[ObjectMapping](originalTransformRule).operator
    val clonedInput = as[ObjectMapping](clonedTransformRule).operator
    originalInput mustBe clonedInput
    val clonedLabel = clonedTransformRule.metaData.label
    clonedLabel mustBe Some(s"Copy of target:address")
  }

  "Copy root mapping rule as child rule of itself" in {
    postRequest(s"$baseUrl/transform/tasks/$project/$task/rule/$ROOT_RULE_ID/rules/copyFrom?" +
        s"sourceProject=$project&sourceTask=$task&sourceRule=$ROOT_RULE_ID")
    retrieveRuleOrder() mustBe Seq("directRule2", OBJECT_RULE_ID, insertedAfterRuleId, insertedAfterRuleId + "1",
      "directRule", OBJECT_RULE_ID + "1", ROOT_RULE_ID + "1")
    val originalTransformRule = rootPropertyRule(ROOT_RULE_ID)
    val clonedTransformRule = rootPropertyRule(ROOT_RULE_ID + "1")
    originalTransformRule.rules.allRules.size mustBe (clonedTransformRule.rules.allRules.size + 1)
    val originalUriRule = as[RootMappingRule](originalTransformRule).rules.uriRule.get.operator
    val clonedUriRule = as[ObjectMapping](clonedTransformRule).rules.uriRule.get.operator
    clonedUriRule mustBe originalUriRule
    val clonedLabel = clonedTransformRule.metaData.label
    clonedLabel mustBe Some(s"Copy of target:Person")
    clonedTransformRule.target mustBe Some(MappingTarget(TransformTaskApi.ROOT_COPY_TARGET_PROPERTY, ValueType.URI))
  }

  private def as[T](obj: Any): T = {
    obj.asInstanceOf[T]
  }

  private def rootPropertyRule(ruleId: String): TransformRule = {
    if(ruleId == ROOT_RULE_ID) {
      transformTask.data.mappingRule
    } else {
      transformTask.data.rules.propertyRules.
          find(_.id.toString == ruleId).getOrElse(throw new RuntimeException(s"Property rule '$ruleId' not found!"))
    }
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

  "Convert value rules to complex rules if convertToComplex=true" in {
    jsonGetRequest(s"$baseUrl/transform/tasks/$project/$task/rule/directRule?convertToComplex=true") mustMatchJson {
      """
        {
          "id": "directRule",
          "mappingTarget": {
              "isAttribute": true,
              "isBackwardProperty": false,
              "uri": "target:firstName",
              "valueType": {
                  "nodeType": "StringValueType"
              }
          },
          "metadata": {
              "description": "updated direct rule description",
              "label": "updated direct rule label"
          },
          "operator": {
              "id": "directRule",
              "path": "source:firstName",
              "type": "pathInput"
          },
          "sourcePaths": [
              "source:firstName"
          ],
          "type": "complex",
          "layout":{"nodePositions":{}},
          "uiAnnotations":{"stickyNotes":[]}
        }
      """
    }
  }

  "Delete mapping rule" in {
    val request = client.url(s"$baseUrl/transform/tasks/$project/$task/rule/objectRule")
    val response = request.delete()
    checkResponse(response)
  }

  "Return 404 if a requested rule does not exist" in {
    var request = client.url(s"$baseUrl/transform/tasks/$project/$task/rule/objectRule")
    request = request.addHttpHeaders("Accept" -> "application/json")
    val response = Await.result(request.get(), 100.seconds)
    response.status mustBe 404
  }

  "Return 400 if trying to store an invalid URI pattern rule" in {
    for((invalidPattern, errorMustInclude) <- Seq(("urn:{{", "illegal character"), ("http://example.org/not valid", "invalid uri pattern found"))) {
      val postResponse = client.url(s"$baseUrl/transform/tasks/$project/$task/rule/root/rules").post(Json.parse(
        s"""
        {
          "type": "uri",
          "pattern": "$invalidPattern"
        }
      """
      ))
      val postJsonError = checkResponseExactStatusCode(postResponse, BAD_REQUEST).json
      postJsonError.toString().toLowerCase must include (errorMustInclude)
      val putResponse = client.url(s"$baseUrl/transform/tasks/$project/$task/rule/root").put(Json.parse(
        s"""
        {
          "rules": {
            "uriRule": {
              "type": "uri",
              "pattern": "$invalidPattern"
            }
          }
        }
      """
      ))
      val putJsonError = checkResponseExactStatusCode(putResponse, BAD_REQUEST).json
      putJsonError.toString().toLowerCase must include (errorMustInclude)
    }
  }
}
