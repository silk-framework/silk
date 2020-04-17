package controllers.workspace

import helper.IntegrationTestTrait
import org.scalatest.MustMatchers
import org.scalatestplus.play.PlaySpec
import org.silkframework.config.MetaData
import org.silkframework.dataset.DatasetSpec
import org.silkframework.dataset.rdf.SparqlEndpointDatasetParameter
import org.silkframework.plugins.dataset.rdf.datasets.{InMemoryDataset, SparqlDataset}
import org.silkframework.plugins.dataset.rdf.tasks.SparqlSelectCustomTask
import org.silkframework.runtime.plugin.PluginRegistry
import org.silkframework.serialization.json.JsonSerializers.{DATA, ID, PARAMETERS, TASKTYPE, TYPE}
import org.silkframework.workspace.TestCustomTask
import play.api.http.Status
import play.api.libs.json._

class TaskApiTest extends PlaySpec with IntegrationTestTrait with MustMatchers {

  private val project = "project"

  override def workspaceProviderId: String = "inMemory"

  protected override def routes = Some(classOf[test.Routes])

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
    val request = client.url(s"$baseUrl/workspace/projects/$project/tasks")
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

  "post task without ID, but label" in {
    val request = client.url(s"$baseUrl/workspace/projects/$project/tasks")
    val response = request.post(
      Json.obj(
        "metadata" ->
            Json.obj(
              "label" -> "some label"
            ),
        DATA -> Json.obj(
          "uriProperty" -> "URI",
          TASKTYPE -> "Dataset",
          TYPE -> "internal",
          PARAMETERS ->
              Json.obj(
                "graphUri" -> "urn:dataset2"
              )
        )
      )
    )
    val r = checkResponse(response)
    val location = r.headerValues("Location").headOption.getOrElse("")
    location must endWith ("somelabel")
    workspaceProject(project).anyTaskOption(location.split("/").last) mustBe defined
  }

  "post dataset task with existing identifier" in {
    val request = client.url(s"$baseUrl/workspace/projects/$project/tasks")
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
    var request = client.url(s"$baseUrl/workspace/projects/$project/tasks/$datasetId")
    request = request.addHttpHeaders("Accept" -> "application/json")
    val response = checkResponse(request.get())
    val json = response.json
    val metaData = (json \ "metadata").get
    (metaData \ "label").as[String] mustBe "label 1"
    (metaData \ "description").as[String] mustBe "description 1"
    (metaData \ "modified").asOpt[String] mustBe defined
    (metaData \ "created").asOpt[String] mustBe defined
    (json \ ID).as[String] mustBe datasetId
    (json \ "project").as[String] mustBe project
    (json \ TASKTYPE).as[String] mustBe "Dataset"
    (json \ DATA).as[JsObject] mustBe Json.obj(
      TASKTYPE -> "Dataset",
      TYPE -> "internal",
      PARAMETERS -> Json.obj(
        "graphUri" -> "urn:dataset1"
      )
    )
  }

  "update dataset task" in {
    var request = client.url(s"$baseUrl/workspace/projects/$project/tasks/$datasetId")
    request = request.addHttpHeaders("Accept" -> "application/json")
    val response = request.put(
      Json.obj(
        ID -> datasetId,
        "metadata" ->
          Json.obj(
            "label" -> "label 2",
            "description" -> "description 2"
          ),
        DATA -> Json.obj(
          "uriProperty" -> "URI",
          TASKTYPE -> "Dataset",
          TYPE -> "internal",
          PARAMETERS ->
            Json.obj(
              "graphUri" -> "urn:dataset2"
            )
        )
      )
    )
    checkResponse(response)
  }

  "get updated dataset" in {
    var request = client.url(s"$baseUrl/workspace/projects/$project/tasks/$datasetId")
    request = request.addHttpHeaders("Accept" -> "application/xml")
    val response = checkResponse(request.get())
    val xml = response.xml

    (xml \ "@id").text mustBe datasetId
    (xml \ "@uriProperty").text mustBe "URI"
    (xml \ "MetaData" \ "Label").text mustBe "label 2"
    (xml \ "MetaData" \ "Description").text mustBe "description 2"
    (xml \ "Param").text mustBe "urn:dataset2"
  }

  "post transform task" in {
    val request = client.url(s"$baseUrl/workspace/projects/$project/tasks")
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
    var request = client.url(s"$baseUrl/workspace/projects/$project/tasks/$transformId")
    request = request.addHttpHeaders("Accept" -> "application/json")
    val response = checkResponse(request.get())
    val json = response.json
    (json \ ID).get mustBe JsString(transformId)
    (json \ TASKTYPE).get mustBe JsString("Transform")
    (json \ DATA \ PARAMETERS \ "selection" \ "inputId").as[String] mustBe datasetId
    (json \ DATA \ PARAMETERS \ "selection" \ "typeUri").as[String] mustBe typeUri
    (json \ DATA \ PARAMETERS \ "mappingRule" \ "rules" \ "uriRule" \ "operator").as[JsObject].toString mustBe
        """{"type":"transformInput","id":"constant","function":"constant","inputs":[],"parameters":{"value":"http://example.org/"}}"""
  }

  "get transform task" in {
   checkTransformTask("")
  }

  val TRANSFORM_OUTPUT_DATASET = "outData"
  val OUTPUT_DATASET_LABEL = "output dataset"

  "patch transform task" in {
    createCsvFileDataset(project, TRANSFORM_OUTPUT_DATASET, "none.csv")
    retrieveOrCreateProject(project).anyTask(TRANSFORM_OUTPUT_DATASET).updateMetaData(MetaData(label = OUTPUT_DATASET_LABEL))
    val updateJson = s"""{
                       |    "id": "$transformId",
                       |    "data": {
                       |      "parameters": {
                       |        "output": "$TRANSFORM_OUTPUT_DATASET",
                       |        "selection": {
                       |          "inputId": "$datasetId",
                       |          "restriction": "",
                       |          "typeUri": "someType"
                       |        },
                       |        "targetVocabularies": [],
                       |        "taskType": "Transform"
                       |      }
                       |    }
                       |}""".stripMargin
    val request = client.url(s"$baseUrl/workspace/projects/$project/tasks/$transformId")
    val response = request.patch(Json.parse(updateJson))
    checkResponse(response)
  }

  "check that transform task has been updated correctly" in {
    checkTransformTask("someType")
  }

  "post linking task" in {
    val request = client.url(s"$baseUrl/workspace/projects/$project/tasks")
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
    var request = client.url(s"$baseUrl/workspace/projects/$project/tasks/$linkTaskId")
    request = request.addHttpHeaders("Accept" -> "application/json")
    val response = checkResponse(request.get())

    (response.json \ ID).get mustBe JsString(linkTaskId)
    (response.json \ DATA \ PARAMETERS \ "source" \ "typeUri").get mustBe JsString("<http://dbpedia.org/ontology/Film>")
    (response.json \ DATA \ PARAMETERS \ "target" \ "typeUri").get mustBe JsString("<http://data.linkedmdb.org/resource/movie/film>")
    (response.json \ DATA \ PARAMETERS \ "rule" \ "linkType").get mustBe JsString("owl:sameAs")
  }

  "patch linking task" in {
    val updateJson =
      s"""
         | {
         |  "id": "$linkTaskId",
         |  "data": {
         |    "parameters": {
         |      "source": {
         |        "inputId": "$datasetId",
         |        "typeUri": "owl:Class",
         |        "restriction": ""
         |      },
         |      "target": {
         |        "inputId": "$datasetId",
         |        "typeUri": "<urn:schema:targetType>",
         |        "restriction": ""
         |      }
         |    }
         |  }
         | }
       """.stripMargin
    val request = client.url(s"$baseUrl/workspace/projects/$project/tasks/$linkTaskId")
    val response = request.patch(Json.parse(updateJson))
    checkResponse(response)
  }

  "check that linking task has been updated correctly" in {
    var request = client.url(s"$baseUrl/workspace/projects/$project/tasks/$linkTaskId")
    request = request.addHttpHeaders("Accept" -> "application/json")
    val response = checkResponse(request.get())

    (response.json \ ID).get mustBe JsString(linkTaskId)
    (response.json \ DATA \ PARAMETERS \ "source" \ "typeUri").get mustBe JsString("owl:Class")
    (response.json \ DATA \ PARAMETERS \ "target" \ "typeUri").get mustBe JsString("<urn:schema:targetType>")
    (response.json \ DATA \ PARAMETERS \ "rule" \ "linkType").get mustBe JsString("owl:sameAs")
  }

  "post workflow task" in {
    val request = client.url(s"$baseUrl/workspace/projects/$project/tasks")
    val response = request.post(
      <Workflow id={workflowId}>
      </Workflow>
    )
    checkResponse(response)
  }

  "get workflow task" in {
    var request = client.url(s"$baseUrl/workspace/projects/$project/tasks/$workflowId")
    request = request.addHttpHeaders("Accept" -> "application/xml")
    val response = checkResponse(request.get())

    (response.xml \ "@id").text mustBe workflowId
  }

  "post custom task" in {
    val request = client.url(s"$baseUrl/workspace/projects/$project/tasks")
    val response = request.post(
      <CustomTask id={customTaskId} type="test">
        <Param name="stringParam" value="someValue"/>
        <Param name="numberParam" value="1"/>
      </CustomTask>
    )
    checkResponse(response)
  }

  "get custom task" in {
    var request = client.url(s"$baseUrl/workspace/projects/$project/tasks/$customTaskId")
    request = request.addHttpHeaders("Accept" -> "application/xml")
    val response = checkResponse(request.get())

    (response.xml \ "@id").text mustBe customTaskId
    (response.xml \ "@type").text mustBe "test"
    (response.xml \ "Param").filter(p => (p \ "@name").text == "stringParam").text mustBe "someValue"
  }

  "get tasks with parameter value labels" in {
    val datasetLabel = "I am a dataset"
    retrieveOrCreateProject(project).anyTask(datasetId).updateMetaData(MetaData(label = datasetLabel))
    def taskValuesWithLabel(taskId: String): Seq[(JsValue, Option[String])] = {
      val parameters = (checkResponse(client.url(s"$baseUrl/workspace/projects/$project/tasks/$taskId?withLabels=true").
          withHttpHeaders("Accept" -> "application/json").
          get()).json.as[JsObject] \ DATA \ PARAMETERS).as[JsObject].fields
      parameters.map(p => ((p._2 \ "value").as[JsValue], (p._2 \ "label").asOpt[String]))
    }
    val sparqlSelect = "sparqlSelect"
    val sparqlDataset = "sparqlDataset"
    val inMemoryDataset = "inMemoryDataset"
    val inMemoryDatasetLabel = "An in-memory dataset"
    val p = workspaceProject(project)
    // Add tasks
    p.addAnyTask(inMemoryDataset, DatasetSpec(InMemoryDataset()), MetaData(label = inMemoryDatasetLabel))
    p.addAnyTask(sparqlSelect, SparqlSelectCustomTask("SELECT * WHERE {?s ?p ?o}", optionalInputDataset = SparqlEndpointDatasetParameter(inMemoryDataset)))
    p.addAnyTask(sparqlDataset, DatasetSpec(SparqlDataset("http://endpoint")))
    // Check tasks
    taskValuesWithLabel(sparqlSelect).filter(_._2.isDefined) mustBe Seq(JsString(inMemoryDataset) -> Some(inMemoryDatasetLabel))
    taskValuesWithLabel(sparqlDataset).filter(_._2.isDefined) mustBe Seq(JsString("parallel") -> Some("parallel"))
    taskValuesWithLabel(workflowId) // Just check that it returns anything
    taskValuesWithLabel(linkTaskId)
    val transformParameters = taskValuesWithLabel(transformId)
    transformParameters.flatMap(_._2) mustBe Seq(OUTPUT_DATASET_LABEL)
    // Nested values must also have a label
    transformParameters.flatMap(p => (p._1 \ "inputId" \ "value").asOpt[JsString]) mustBe Seq(JsString(datasetId))
    transformParameters.flatMap(p => (p._1 \ "inputId" \ "label").asOpt[JsString]) mustBe Seq(JsString(datasetLabel))
    // Remove tasks
    for(taskId <- Seq(inMemoryDataset, sparqlDataset, sparqlSelect)) {
      p.removeAnyTask(taskId, false)
    }
  }

  "copy endpoint" should {

    val targetProject = "targetProject"

    "simulate copying a task in a dry run" in {
      createProject(targetProject)

      val response = client.url(s"$baseUrl/workspace/projects/$project/tasks/$transformId/copy")
        .post(Json.parse(
          s""" {
            |    "targetProject": "$targetProject",
            |    "dryRun": true
            |  }
          """.stripMargin
        ))

      val responseJson = checkResponse(response).json
      (responseJson \ "copiedTasks").asStringArray must contain theSameElementsAs Seq(datasetId, transformId, TRANSFORM_OUTPUT_DATASET)
      (responseJson \ "overwrittenTasks").asStringArray mustBe Seq.empty

      // Assert that no tasks have been copied
      val projectResponse = client.url(s"$baseUrl/workspace/projects/$targetProject").get()
      val projectJson = checkResponse(projectResponse).json
      (projectJson \ "tasks" \ "transform").asStringArray mustBe Seq.empty
    }

    "copy a task" in {
      val response = client.url(s"$baseUrl/workspace/projects/$project/tasks/$transformId/copy")
        .post(Json.parse(
          s""" {
             |    "targetProject": "$targetProject",
             |    "dryRun": false
             |  }
          """.stripMargin
        ))

      val responseJson = checkResponse(response).json
      (responseJson \ "copiedTasks").asStringArray must contain theSameElementsAs Seq(datasetId, transformId, TRANSFORM_OUTPUT_DATASET)
      (responseJson \ "overwrittenTasks").asStringArray mustBe Seq.empty

      // Assert that tasks have been copied
      val projectResponse = client.url(s"$baseUrl/workspace/projects/$targetProject").get()
      val projectJson = checkResponse(projectResponse).json
      (projectJson \ "tasks" \ "dataset").asStringArray.toSet mustBe Set(datasetId, TRANSFORM_OUTPUT_DATASET)
      (projectJson \ "tasks" \ "transform").asStringArray mustBe Seq(transformId)
    }

    "overwrite tasks when copying" in {
      // Change the label of a dataset in the source project
      val updateDatasetRequest = client.url(s"$baseUrl/workspace/projects/$project/tasks/$datasetId").addHttpHeaders("Accept" -> "application/json")
      checkResponse(updateDatasetRequest.patch(Json.parse("""{ "metadata": { "label": "changed label" } }""")))

      // Copy transform task that references the changed dataset
      val response = client.url(s"$baseUrl/workspace/projects/$project/tasks/$transformId/copy")
        .post(Json.parse(
          s""" {
             |    "targetProject": "$targetProject",
             |    "dryRun": false
             |  }
          """.stripMargin
        ))
      val responseJson = checkResponse(response).json
      (responseJson \ "copiedTasks").asStringArray must contain theSameElementsAs Seq(datasetId, transformId, TRANSFORM_OUTPUT_DATASET)
      (responseJson \ "overwrittenTasks").asStringArray must contain theSameElementsAs Seq(datasetId, transformId, TRANSFORM_OUTPUT_DATASET)

      // Assert that the dataset has been overwritten
      val datasetResponse = client.url(s"$baseUrl/workspace/projects/$targetProject/tasks/$datasetId")
                                .addHttpHeaders("Accept" -> "application/json")
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
