package controllers.workspace

import helper.IntegrationTestTrait
import org.scalatestplus.play.PlaySpec
import org.silkframework.runtime.plugin.PluginRegistry
import org.silkframework.workspace.TestCustomTask
import play.api.http.Status
import play.api.libs.json._
import play.api.libs.ws.WS

class TaskApiTest extends PlaySpec with IntegrationTestTrait {

  private val project = "project"

  override def workspaceProvider: String = "inMemory"

  protected override def routes = Some("test.Routes")

  private val datasetId = "testDataset"

  private val transformId = "testTransform"

  private val linkTaskId = "testLinkTask"

  private val workflowId = "testWorkflow"

  private val customTaskId = "testCustomTask"

  "setup" in {
    PluginRegistry.registerPlugin(classOf[TestCustomTask])
    createProject(project)
  }

  "post dataset task" in {
    val request = WS.url(s"$baseUrl/workspace/projects/$project/tasks")
    val response = request.post(
      <Dataset id={datasetId} type="internal">
        <MetaData>
          <Label>label 1</Label>
          <Description>description 1</Description>
          <Modified>2018-03-08T13:05:40.347Z</Modified>
        </MetaData>
        <Param name="graphUri" value="urn:dataset1"/>
      </Dataset>
    )
    checkResponse(response)
  }

  "post dataset task with existing identifier" in {
    val request = WS.url(s"$baseUrl/workspace/projects/$project/tasks")
    val response = request.post(
      <Dataset id={datasetId} type="internal">
        <MetaData>
          <Label>label 1</Label>
          <Description>description 1</Description>
          <Modified>2018-03-08T13:05:40.347Z</Modified>
        </MetaData>
        <Param name="graphUri" value="urn:dataset1"/>
      </Dataset>
    )
    checkResponseCode(response, Status.CONFLICT)
  }

  "get dataset task" in {
    var request = WS.url(s"$baseUrl/workspace/projects/$project/tasks/$datasetId")
    request = request.withHeaders("Accept" -> "application/json")
    val response = checkResponse(request.get())
    response.json mustBe
      Json.obj(
        "id" -> datasetId,
        "project" -> project,
        "metadata" ->
          Json.obj(
            "label" -> "label 1",
            "description" -> "description 1",
            "modified" -> "2018-03-08T13:05:40.347Z"
          ),
        "taskType" -> "Dataset",
        "data" -> Json.obj(
          "taskType" -> "Dataset",
          "type" -> "internal",
          "parameters" -> Json.obj(
            "graphUri" -> "urn:dataset1"
          )
        )
      )
  }

  "update dataset task" in {
    var request = WS.url(s"$baseUrl/workspace/projects/$project/tasks/$datasetId")
    request = request.withHeaders("Accept" -> "application/json")
    val response = request.put(
      Json.obj(
        "id" -> datasetId,
        "metadata" ->
          Json.obj(
            "label" -> "label 2",
            "description" -> "description 2"
          ),
        "data" -> Json.obj(
          "uriProperty" -> "URI",
          "taskType" -> "Dataset",
          "type" -> "internal",
          "parameters" ->
            Json.obj(
              "graphUri" -> "urn:dataset2"
            )
        )
      )
    )
    checkResponse(response)
  }

  "get updated dataset" in {
    var request = WS.url(s"$baseUrl/workspace/projects/$project/tasks/$datasetId")
    request = request.withHeaders("Accept" -> "application/xml")
    val response = checkResponse(request.get())
    val xml = response.xml

    (xml \ "@id").text mustBe datasetId
    (xml \ "@uriProperty").text mustBe "URI"
    (xml \ "MetaData" \ "Label").text mustBe "label 2"
    (xml \ "MetaData" \ "Description").text mustBe "description 2"
    (xml \ "Param").text mustBe "urn:dataset2"
  }

  "post transform task" in {
    val request = WS.url(s"$baseUrl/workspace/projects/$project/tasks")
    val response = request.post(
      <TransformSpec id={transformId}>
        <SourceDataset dataSource={datasetId} var="a" typeUri="" />
        <TransformRule name="rule" targetProperty="">
            <TransformInput id="constant" function="constant">
              <Param name="value" value="http://example.org/"/>
            </TransformInput>
        </TransformRule>
      </TransformSpec>
    )
    checkResponse(response)
  }

  private def checkTransformTask(typeUri: String): Unit = {
    var request = WS.url(s"$baseUrl/workspace/projects/$project/tasks/$transformId")
    request = request.withHeaders("Accept" -> "application/json")
    val response = checkResponse(request.get())
    val json = response.json
    (json \ "id").get mustBe JsString(transformId)
    (json \ "taskType").get mustBe JsString("Transform")
    (json \ "data" \ "selection" \ "inputId").get mustBe JsString(datasetId)
    (json \ "data" \ "selection" \ "typeUri").as[String] mustBe typeUri
    (json \ "data" \ "root" \ "rules" \ "uriRule" \ "operator").as[JsObject].toString mustBe
        """{"type":"transformInput","id":"constant","function":"constant","inputs":[],"parameters":{"value":"http://example.org/"}}"""
  }

  "get transform task" in {
   checkTransformTask("")
  }

  "patch transform task" in {
    val updateJson = s"""{
                       |    "id": "$transformId",
                       |    "data": {
                       |      "outputs": [],
                       |      "selection": {
                       |        "inputId": "$datasetId",
                       |        "restriction": "",
                       |        "typeUri": "someType"
                       |      },
                       |      "targetVocabularies": [],
                       |      "taskType": "Transform"
                       |    }
                       |}""".stripMargin
    val request = WS.url(s"$baseUrl/workspace/projects/$project/tasks/$transformId")
    val response = request.patch(Json.parse(updateJson))
    checkResponse(response)
  }

  "check that transform task has been updated correctly" in {
    checkTransformTask("someType")
  }

  "post linking task" in {
    val request = WS.url(s"$baseUrl/workspace/projects/$project/tasks")
    val response = request.post(
      <Interlink id={linkTaskId}>
        <SourceDataset dataSource={datasetId} var="a" typeUri="http://dbpedia.org/ontology/Film">
          <RestrictTo>
          </RestrictTo>
        </SourceDataset>
        <TargetDataset dataSource={datasetId} var="b" typeUri="http://data.linkedmdb.org/resource/movie/film">
          <RestrictTo>
          </RestrictTo>
        </TargetDataset>
        <LinkageRule linkType="owl:sameAs">
          <Aggregate id="combineSimilarities" required="false" weight="1" type="min">
            <Compare id="compareTitles" required="false" weight="1" metric="levenshteinDistance" threshold="0.0" indexing="true">
              <TransformInput id="toLowerCase1" function="lowerCase">
                <Input id="movieTitle1" path="/&lt;http://xmlns.com/foaf/0.1/name&gt;"/>
              </TransformInput>
              <TransformInput id="toLowerCase2" function="lowerCase">
                <Input id="movieTitle2" path="/&lt;http://www.w3.org/2000/01/rdf-schema#label&gt;"/>
              </TransformInput>
            </Compare>
          </Aggregate>
        </LinkageRule>
      </Interlink>
    )
    checkResponse(response)
  }

  "get linking task" in {
    var request = WS.url(s"$baseUrl/workspace/projects/$project/tasks/$linkTaskId")
    request = request.withHeaders("Accept" -> "application/json")
    val response = checkResponse(request.get())

    (response.json \ "id").get mustBe JsString(linkTaskId)
    (response.json \ "data" \ "source" \ "typeUri").get mustBe JsString("<http://dbpedia.org/ontology/Film>")
    (response.json \ "data" \ "target" \ "typeUri").get mustBe JsString("<http://data.linkedmdb.org/resource/movie/film>")
    (response.json \ "data" \ "rule" \ "linkType").get mustBe JsString("owl:sameAs")
  }

  "patch linking task" in {
    val updateJson =
      s"""
         | {
         |  "id": "$linkTaskId",
         |  "data": {
         |    "source": {
         |      "inputId": "$datasetId",
         |      "typeUri": "owl:Class",
         |      "restriction": ""
         |    },
         |    "target": {
         |      "inputId": "$datasetId",
         |      "typeUri": "<urn:schema:targetType>",
         |      "restriction": ""
         |    }
         |  }
         | }
       """.stripMargin
    val request = WS.url(s"$baseUrl/workspace/projects/$project/tasks/$linkTaskId")
    val response = request.patch(Json.parse(updateJson))
    checkResponse(response)
  }

  "check that linking task has been updated correctly" in {
    var request = WS.url(s"$baseUrl/workspace/projects/$project/tasks/$linkTaskId")
    request = request.withHeaders("Accept" -> "application/json")
    val response = checkResponse(request.get())

    (response.json \ "id").get mustBe JsString(linkTaskId)
    (response.json \ "data" \ "source" \ "typeUri").get mustBe JsString("owl:Class")
    (response.json \ "data" \ "target" \ "typeUri").get mustBe JsString("<urn:schema:targetType>")
    (response.json \ "data" \ "rule" \ "linkType").get mustBe JsString("owl:sameAs")
  }

  "post workflow task" in {
    val request = WS.url(s"$baseUrl/workspace/projects/$project/tasks")
    val response = request.post(
      <Workflow id={workflowId}>
      </Workflow>
    )
    checkResponse(response)
  }

  "get workflow task" in {
    var request = WS.url(s"$baseUrl/workspace/projects/$project/tasks/$workflowId")
    request = request.withHeaders("Accept" -> "application/xml")
    val response = checkResponse(request.get())

    (response.xml \ "@id").text mustBe workflowId
  }

  "post custom task" in {
    val request = WS.url(s"$baseUrl/workspace/projects/$project/tasks")
    val response = request.post(
      <CustomTask id={customTaskId} type="test">
        <Param name="stringParam" value="someValue"/>
        <Param name="numberParam" value="1"/>
      </CustomTask>
    )
    checkResponse(response)
  }

  "get custom task" in {
    var request = WS.url(s"$baseUrl/workspace/projects/$project/tasks/$customTaskId")
    request = request.withHeaders("Accept" -> "application/xml")
    val response = checkResponse(request.get())

    (response.xml \ "@id").text mustBe customTaskId
    (response.xml \ "@type").text mustBe "test"
    (response.xml \ "Param").filter(p => (p \ "@name").text == "stringParam").text mustBe "someValue"
  }

  "copy endpoint" should {

    val targetProject = "targetProject"

    "simulate copying a task in a dry run" in {
      createProject(targetProject)

      val response = WS.url(s"$baseUrl/workspace/projects/$project/tasks/$transformId/copy")
        .post(Json.parse(
          s""" {
            |    "targetProject": "$targetProject",
            |    "dryRun": true
            |  }
          """.stripMargin
        ))

      val responseJson = checkResponse(response).json
      (responseJson \ "copiedTasks").asStringArray must contain theSameElementsAs Seq(datasetId, transformId)
      (responseJson \ "overwrittenTasks").asStringArray mustBe Seq.empty

      // Assert that no tasks have been copied
      val projectResponse = WS.url(s"$baseUrl/workspace/projects/$targetProject").get()
      val projectJson = checkResponse(projectResponse).json
      (projectJson \ "tasks" \ "transform").asStringArray mustBe Seq.empty
    }

    "copy a task" in {
      val response = WS.url(s"$baseUrl/workspace/projects/$project/tasks/$transformId/copy")
        .post(Json.parse(
          s""" {
             |    "targetProject": "$targetProject",
             |    "dryRun": false
             |  }
          """.stripMargin
        ))

      val responseJson = checkResponse(response).json
      (responseJson \ "copiedTasks").asStringArray must contain theSameElementsAs Seq(datasetId, transformId)
      (responseJson \ "overwrittenTasks").asStringArray mustBe Seq.empty

      // Assert that tasks have been copied
      val projectResponse = WS.url(s"$baseUrl/workspace/projects/$targetProject").get()
      val projectJson = checkResponse(projectResponse).json
      (projectJson \ "tasks" \ "dataset").asStringArray mustBe Seq(datasetId)
      (projectJson \ "tasks" \ "transform").asStringArray mustBe Seq(transformId)
    }

    "overwrite tasks when copying" in {
      // Change the label of a dataset in the source project
      val updateDatasetRequest = WS.url(s"$baseUrl/workspace/projects/$project/tasks/$datasetId").withHeaders("Accept" -> "application/json")
      checkResponse(updateDatasetRequest.patch(Json.parse("""{ "metadata": { "label": "changed label" } }""")))

      // Copy transform task that references the changed dataset
      val response = WS.url(s"$baseUrl/workspace/projects/$project/tasks/$transformId/copy")
        .post(Json.parse(
          s""" {
             |    "targetProject": "$targetProject",
             |    "dryRun": false
             |  }
          """.stripMargin
        ))
      val responseJson = checkResponse(response).json
      (responseJson \ "copiedTasks").asStringArray must contain theSameElementsAs Seq(datasetId, transformId)
      (responseJson \ "overwrittenTasks").asStringArray must contain theSameElementsAs Seq(datasetId, transformId)

      // Assert that the dataset has been overwritten
      val datasetResponse = WS.url(s"$baseUrl/workspace/projects/$targetProject/tasks/$datasetId")
                                .withHeaders("Accept" -> "application/json")
                                .get()
      val datasetJson = checkResponse(datasetResponse).json
      (datasetJson \ "metadata" \ "label").as[JsString].value mustBe "changed label"
    }

  }

  /**
    * Convenience methods for reading JSON values to make the tests more readable.
    */
  implicit class JsReadableHelpers(value: JsReadable) {
    def asStringArray: Seq[String] = {
      value.as[JsArray].value.map(_.as[JsString].value)
    }
  }

}
