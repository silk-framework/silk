package controllers.transform

import org.silkframework.entity.ValueType
import org.silkframework.rule._
import org.silkframework.serialization.json.JsonSerializers.{DATA, PARAMETERS}
import org.silkframework.workspace.ProjectTask
import play.api.libs.json.{JsArray, JsNull, JsObject, JsString, JsValue, Json}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.xml.XML

class TransformTaskApiTest extends TransformTaskApiTestBase {

  def printResponses = false

  private val OBJECT_RULE_ID = "objectRule"
  private val ROOT_RULE_ID = "root"
  private val expectedSearchTreeRuleOrder = Seq(
    ROOT_RULE_ID,
    "directRule",
    OBJECT_RULE_ID,
    "addressStreetRule",
    "addressLocationRule",
    "locationLatitudeRule",
    "directRule2"
  )
  private val expectedSearchValueRuleOrder = Seq(
    "directRule",
    "addressStreetRule",
    "locationLatitudeRule",
    "directRule2"
  )

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
    (json \ "rules" \ "propertyRules").as[JsArray].value mustBe Array.empty
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
      objectRuleJson(OBJECT_RULE_ID, "source:address", "target:address")
    }
    (json \ "metadata" \ "label").isDefined mustBe false
    (json \ "id").as[String] mustBe OBJECT_RULE_ID
  }

  "Append direct mapping rule to object mapping" in {
    jsonPostRequest(s"$baseUrl/transform/tasks/$project/$task/rule/$OBJECT_RULE_ID/rules") {
      directRuleJson("addressStreetRule", "source:street", "target:street")
    }
  }

  "Append nested object mapping rule to object mapping" in {
    jsonPostRequest(s"$baseUrl/transform/tasks/$project/$task/rule/$OBJECT_RULE_ID/rules") {
      objectRuleJson("addressLocationRule", "source:location", "target:location")
    }
  }

  "Append direct mapping rule to nested object mapping" in {
    jsonPostRequest(s"$baseUrl/transform/tasks/$project/$task/rule/addressLocationRule/rules") {
      directRuleJson("locationLatitudeRule", "source:lat", "target:lat")
    }
  }

  "Retrieve full mapping rule tree" in {
    jsonGetRequest(s"$baseUrl/transform/tasks/$project/$task/rules") mustBe
      expectedRootRuleResponse(
        uriPattern = "http://example.org/{PersonID}",
        typeRuleId = "explicitlyDefinedId",
        typeUri = "target:Person",
        propertyRules = Seq(
          expectedDirectRuleResponse(
            id = "directRule",
            sourcePath = "source:name",
            targetUri = "target:name",
            metadata = Json.obj(
              "description" -> "updated direct rule description",
              "label" -> "updated direct rule label"
            )
          ),
          expectedObjectRuleResponse(
            id = OBJECT_RULE_ID,
            sourcePath = "source:address",
            targetUri = "target:address",
            propertyRules = Seq(
              expectedDirectRuleResponse("addressStreetRule", "source:street", "target:street"),
              expectedObjectRuleResponse(
                id = "addressLocationRule",
                sourcePath = "source:location",
                targetUri = "target:location",
                propertyRules = Seq(
                  expectedDirectRuleResponse("locationLatitudeRule", "source:lat", "target:lat")
                )
              )
            )
          )
        )
      )
  }

  "Retrieve a single mapping rule" in {
    jsonGetRequest(s"$baseUrl/transform/tasks/$project/$task/rule/objectRule") mustBe
      expectedObjectRuleResponse(
        id = OBJECT_RULE_ID,
        sourcePath = "source:address",
        targetUri = "target:address",
        propertyRules = Seq(
          expectedDirectRuleResponse("addressStreetRule", "source:street", "target:street"),
          expectedObjectRuleResponse(
            id = "addressLocationRule",
            sourcePath = "source:location",
            targetUri = "target:location",
            propertyRules = Seq(
              expectedDirectRuleResponse("locationLatitudeRule", "source:lat", "target:lat")
            )
          )
        )
      )
  }

  "Append another direct mapping rule to root" in {
    jsonPostRequest(s"$baseUrl/transform/tasks/$project/$task/rule/root/rules") {
      directRuleJson(
        id = "directRule2",
        sourcePath = "/source:lastName",
        targetUri = "target:lastName",
        metadata = Some(
          """"metadata" : {
            |  "label" : "direct rule label",
            |  "description" : "direct rule description"
            |}""".stripMargin
        )
      )
    }
  }

  "Search rules without a search term in mapping tree order without formatting by default" in {
    val response = searchRules { searchRulesRequestJson() }

    filteredCompletionValues(response, expectedSearchTreeRuleOrder) mustBe expectedSearchTreeRuleOrder

    completionLabelByValue(response)(ROOT_RULE_ID) mustBe "target:Person"
    completionLabelByValue(response)("directRule") mustBe "updated direct rule label"
    completionLabelByValue(response)(OBJECT_RULE_ID) mustBe "target:address"
    completionLabelByValue(response)("addressStreetRule") mustBe "target:street"
    completionLabelByValue(response)("addressLocationRule") mustBe "target:location"
    completionLabelByValue(response)("locationLatitudeRule") mustBe "target:lat"
    completionLabelByValue(response)("directRule2") mustBe "direct rule label"
  }

  "Search rules without a search term with tree formatting if requested" in {
    val response = searchRules { searchRulesRequestJson(format = true) }

    val labelsByValue = completionLabelByValue(response)

    filteredCompletionValues(response, expectedSearchTreeRuleOrder) mustBe expectedSearchTreeRuleOrder

    labelsByValue(ROOT_RULE_ID) mustBe "target:Person"
    labelsByValue("directRule") mustBe "├─ updated direct rule label"
    labelsByValue(OBJECT_RULE_ID) mustBe "├─ target:address"
    labelsByValue("addressStreetRule") mustBe "│ ├─ target:street"
    labelsByValue("addressLocationRule") mustBe "│ └─ target:location"
    labelsByValue("locationLatitudeRule") mustBe "│   └─ target:lat"
    labelsByValue("directRule2") mustBe "└─ direct rule label"
  }

  "Search value rules without a search term as a flat list even if formatting is requested" in {
    val response = searchRules { searchRulesRequestJson(valueRulesOnly = true, format = true) }

    val labelsByValue = completionLabelByValue(response)

    filteredCompletionValues(response, expectedSearchValueRuleOrder) mustBe expectedSearchValueRuleOrder

    labelsByValue("directRule") mustBe "updated direct rule label"
    labelsByValue("addressStreetRule") mustBe "target:street"
    labelsByValue("locationLatitudeRule") mustBe "target:lat"
    labelsByValue("directRule2") mustBe "direct rule label"
  }

  "Search rules with a search term using the existing flat display and ranking" in {
    val response = searchRules { searchRulesRequestJson(searchQuery = "target", format = true) }

    val labelsByValue = completionLabelByValue(response)

    completionValues(response) must contain allOf(OBJECT_RULE_ID, "addressStreetRule", "addressLocationRule", "locationLatitudeRule")
    labelsByValue(OBJECT_RULE_ID) mustBe "target:address"
    labelsByValue("addressStreetRule") mustBe "target:street"
    labelsByValue("addressLocationRule") mustBe "target:location"
    labelsByValue("locationLatitudeRule") mustBe "target:lat"
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

  private def searchRules(json: => String) = {
    jsonPostRequest(s"$baseUrl/transform/tasks/$project/$task/rules/search")(json)
  }

  private def directRuleJson(id: String,
                             sourcePath: String,
                             targetUri: String,
                             metadata: Option[String] = None): String = {
    val metadataJson = metadata.map(",\n" + _).getOrElse("")
    s"""{
       |  "type": "direct",
       |  "id": "$id",
       |  "sourcePath": "$sourcePath",
       |  "mappingTarget": {
       |    "uri": "$targetUri",
       |    "valueType": {
       |      "nodeType": "StringValueType"
       |    }
       |  }$metadataJson
       |}""".stripMargin
  }

  private def directRuleJsonWithoutId(sourcePath: String,
                                      targetUri: String,
                                      metadata: Option[String] = None): String = {
    val metadataJson = metadata.map(",\n" + _).getOrElse("")
    s"""{
       |  "type": "direct",
       |  "sourcePath": "$sourcePath",
       |  "mappingTarget": {
       |    "uri": "$targetUri",
       |    "valueType": {
       |      "nodeType": "StringValueType"
       |    }
       |  }$metadataJson
       |}""".stripMargin
  }

  private def objectRuleJson(id: String, sourcePath: String, targetUri: String): String = {
    s"""{
       |  "type": "object",
       |  "id": "$id",
       |  "sourcePath": "$sourcePath",
       |  "mappingTarget": {
       |    "uri": "$targetUri",
       |    "valueType": {
       |      "nodeType": "UriValueType"
       |    }
       |  },
       |  "rules": {
       |    "uriRule": null,
       |    "typeRules": [],
       |    "propertyRules": []
       |  }
       |}""".stripMargin
  }

  private def searchRulesRequestJson(searchQuery: String = "",
                                     limit: Int = 20,
                                     valueRulesOnly: Boolean = false,
                                     format: Boolean = false): String = {
    Json.stringify(
      Json.obj(
        "searchQuery" -> searchQuery,
        "limit" -> limit
      ) ++ JsObject(
        Seq(
          Option.when(valueRulesOnly)("valueRulesOnly" -> Json.toJson(true)),
          Option.when(format)("format" -> Json.toJson(true))
        ).flatten
      )
    )
  }

  private def completionValues(json: JsValue): Seq[String] = {
    json.as[JsArray].value.map(c => (c \ "value").as[String]).toSeq
  }

  private def filteredCompletionValues(json: JsValue, expectedOrder: Seq[String]): Seq[String] = {
    completionValues(json).filter(expectedOrder.toSet)
  }

  private def completionLabelByValue(json: JsValue): Map[String, String] = {
    json.as[JsArray].value.map { c =>
      (c \ "value").as[String] -> (c \ "label").as[String]
    }.toMap
  }

  private def expectedDirectRuleResponse(id: String,
                                         sourcePath: String,
                                         targetUri: String,
                                         metadata: JsObject = Json.obj()): JsObject = {
    Json.obj(
      "type" -> "direct",
      "id" -> id,
      "sourcePath" -> sourcePath,
      "mappingTarget" -> Json.obj(
        "uri" -> targetUri,
        "valueType" -> Json.obj(
          "nodeType" -> "StringValueType"
        ),
        "isBackwardProperty" -> false,
        "isAttribute" -> false
      ),
      "metadata" -> metadata
    )
  }

  private def expectedTypeRuleResponse(id: String,
                                       typeUri: String,
                                       metadata: JsObject = Json.obj()): JsObject = {
    Json.obj(
      "type" -> "type",
      "id" -> id,
      "typeUri" -> typeUri,
      "metadata" -> metadata
    )
  }

  private def expectedUriRuleResponse(pattern: String,
                                      id: String = "uri",
                                      metadata: JsObject = Json.obj()): JsObject = {
    Json.obj(
      "type" -> "uri",
      "id" -> id,
      "pattern" -> pattern,
      "metadata" -> metadata
    )
  }

  private def expectedObjectRuleResponse(id: String,
                                         sourcePath: String,
                                         targetUri: String,
                                         propertyRules: Seq[JsObject] = Seq.empty,
                                         metadata: JsObject = Json.obj()): JsObject = {
    Json.obj(
      "type" -> "object",
      "id" -> id,
      "sourcePath" -> sourcePath,
      "mappingTarget" -> Json.obj(
        "uri" -> targetUri,
        "valueType" -> Json.obj(
          "nodeType" -> "UriValueType"
        ),
        "isBackwardProperty" -> false,
        "isAttribute" -> false
      ),
      "rules" -> Json.obj(
        "uriRule" -> JsNull,
        "typeRules" -> Json.arr(),
        "propertyRules" -> JsArray(propertyRules)
      ),
      "metadata" -> metadata
    )
  }

  private def expectedRootRuleResponse(uriPattern: String,
                                       typeRuleId: String,
                                       typeUri: String,
                                       propertyRules: Seq[JsObject],
                                       metadata: JsObject = Json.obj()): JsObject = {
    Json.obj(
      "type" -> "root",
      "id" -> ROOT_RULE_ID,
      "rules" -> Json.obj(
        "uriRule" -> expectedUriRuleResponse(uriPattern),
        "typeRules" -> Json.arr(expectedTypeRuleResponse(typeRuleId, typeUri)),
        "propertyRules" -> JsArray(propertyRules)
      ),
      "mappingTarget" -> Json.obj(
        "uri" -> "",
        "valueType" -> Json.obj(
          "nodeType" -> "UriValueType"
        ),
        "isBackwardProperty" -> false,
        "isAttribute" -> false
      ),
      "metadata" -> metadata
    )
  }

  private def rootPropertyRule(ruleId: String): TransformRule = {
    if (ruleId == ROOT_RULE_ID) {
      transformTask.data.mappingRule
    } else {
      transformTask.data.rules.propertyRules.
        find(_.id.toString == ruleId).getOrElse(throw new RuntimeException(s"Property rule '$ruleId' not found!"))
    }
  }

  "Append multiple new direct mapping rules without ID at the same time" in {
    val resultsFutures = for (i <- 1 to 10) yield {
      Future(jsonPostRequest(s"$baseUrl/transform/tasks/$project/$task/rule/root/rules") {
        directRuleJsonWithoutId(
          sourcePath = "/source:name",
          targetUri = s"target:name$i",
          metadata = Some(
            s""""metadata" : {
               |  "label" : "direct rule label $i",
               |  "description" : "direct rule description"
               |}""".stripMargin
          )
        )
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
    for ((invalidPattern, errorMustInclude) <- Seq(("urn:{{", "illegal character"), ("http://example.org/not valid", "invalid uri pattern found"))) {
      val postResponse = client.url(s"$baseUrl/transform/tasks/$project/$task/rule/root/rules").post(Json.parse(
        s"""
        {
          "type": "uri",
          "pattern": "$invalidPattern"
        }
      """
      ))
      val postJsonError = checkResponseExactStatusCode(postResponse, BAD_REQUEST).json
      postJsonError.toString().toLowerCase must include(errorMustInclude)
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
      putJsonError.toString().toLowerCase must include(errorMustInclude)
    }
  }
}
