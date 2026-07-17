package controllers.workspace

import helper.IntegrationTestTrait
import org.scalatestplus.play.PlaySpec
import org.silkframework.serialization.json.JsonSerializers.{DATA, ID, PARAMETERS, TASKTYPE, TASK_TYPE_DATASET, TYPE}
import play.api.libs.json.{JsObject, Json}

class DatasetApiTest extends PlaySpec with IntegrationTestTrait {

  private val project = "project"

  override def workspaceProviderId: String = "inMemoryWorkspaceProvider"

  protected override def routes = Some(classOf[testWorkspace.Routes])

  "setup" in {
    createProject(project)
  }

  "add datasets using XML" in {
    val dataset = "dataset1"
    val request = client.url(s"$baseUrl/workspace/projects/$project/datasets/dataset1")
    val response = request.put(
      <Dataset id={dataset} type="internal">
        <MetaData>
          <Label>label 1</Label>
          <Description>description 1</Description>
          <Modified>2018-03-08T15:01:06.609Z</Modified>
        </MetaData>
        <Param name="graphUri" value="urn:dataset1"/>
      </Dataset>
    )
    checkResponse(response)
  }

  "add datasets using JSON" in {
    val dataset = "dataset2"
    var request = client.url(s"$baseUrl/workspace/projects/$project/datasets/$dataset")
    request = request.addHttpHeaders("Accept" -> "application/json")
    val response = request.put(
      Json.obj(
        TASKTYPE -> TASK_TYPE_DATASET,
        ID -> dataset,
        "metadata" ->
          Json.obj(
            "label" -> "label 2",
            "description" -> "description 2"
          ),
        DATA -> Json.obj(
          "uriProperty" -> "URI",
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

  "get dataset using JSON" in {
    val dataset = "dataset1"
    var request = client.url(s"$baseUrl/workspace/projects/$project/datasets/$dataset")
    request = request.addHttpHeaders("Accept" -> "application/json")
    val response = checkResponse(request.get())
    val json = response.json
    val metaData = (json \ "metadata").get
    (metaData \ "label").as[String] mustBe "label 1"
    (metaData \ "description").as[String] mustBe "description 1"
    (metaData \ "modified").asOpt[String] mustBe defined
    (metaData \ "created").asOpt[String] mustBe defined
    (json \ "id").as[String] mustBe dataset
    (json \ "project").as[String] mustBe project
    (json \ TASKTYPE).as[String] mustBe "Dataset"
    (json \ DATA).as[JsObject] mustBe Json.obj(
      TASKTYPE -> "Dataset",
      TYPE -> "internal",
      PARAMETERS -> Json.obj(
        "graphUri" -> "urn:dataset1"
      ),
      "readOnly" -> false
    )
  }

  "get dataset using XML" in {
    val dataset = "dataset2"
    var request = client.url(s"$baseUrl/workspace/projects/$project/datasets/$dataset")
    request = request.addHttpHeaders("Accept" -> "application/xml")
    val response = checkResponse(request.get())
    val xml = response.xml

    (xml \ "@id").text mustBe dataset
    (xml \ "@uriProperty").text mustBe "URI"
    (xml \ "MetaData" \ "Label").text mustBe "label 2"
    (xml \ "MetaData" \ "Description").text mustBe "description 2"
    (xml \ "Param").text mustBe "urn:dataset2"
  }

  "update dataset file" in {
    // Create CSV dataset
    val dataset = "csvDataset"
    val fileName = "test.csv"
    var request = client.url(s"$baseUrl/workspace/projects/$project/datasets/$dataset")
    request = request.addHttpHeaders("Accept" -> "application/json")
    val response = request.put(
      Json.obj(
        TASKTYPE -> TASK_TYPE_DATASET,
        ID -> dataset,
        DATA -> Json.obj(
          TYPE -> "csv",
          PARAMETERS ->
            Json.obj(
              "file" -> fileName
            )
        )
      )
    )
    checkResponse(response)

    // Upload csv
    val testData = "test data"
    val uploadRequest = client.url(s"$baseUrl/workspace/projects/$project/datasets/$dataset/file")
    checkResponse(uploadRequest.put(testData))

    val downloadRequest = client.url(s"$baseUrl/workspace/projects/$project/datasets/$dataset/file")
    val downloadedFile = checkResponse(downloadRequest.get()).body

    downloadedFile mustBe testData
  }

  "update execution variables only when the payload provides them" in {
    val dataset = "execVarsDataset"
    def datasetJson(extraFields: (String, Json.JsValueWrapper)*): JsObject = Json.obj(
      (Seq[(String, Json.JsValueWrapper)](
        TASKTYPE -> TASK_TYPE_DATASET,
        ID -> dataset,
        DATA -> Json.obj(
          TYPE -> "internal",
          PARAMETERS -> Json.obj("graphUri" -> "urn:execVarsDataset")
        )
      ) ++ extraFields): _*
    )
    def executionVariable(value: String): JsObject =
      Json.obj("name" -> "myVar", "value" -> value, "isSensitive" -> false, "scope" -> "execution")
    def storedVariables: Seq[(String, String)] =
      workspaceProject(project).anyTask(dataset).executionVariables.variables.map(v => (v.name, v.value))
    def putDataset(json: JsObject): Unit = {
      checkResponse(client.url(s"$baseUrl/workspace/projects/$project/datasets/$dataset")
        .addHttpHeaders("Accept" -> "application/json").put(json))
    }

    // Create the dataset with an execution variable
    putDataset(datasetJson("executionVariables" -> Json.arr(executionVariable("v1"))))
    storedVariables mustBe Seq(("myVar", "v1"))

    // A PUT without the executionVariables key must leave the stored variables unchanged
    putDataset(datasetJson())
    storedVariables mustBe Seq(("myVar", "v1"))

    // A PUT with the key must replace them
    putDataset(datasetJson("executionVariables" -> Json.arr(executionVariable("v2"))))
    storedVariables mustBe Seq(("myVar", "v2"))

    // A PUT with an explicit empty array must clear them
    putDataset(datasetJson("executionVariables" -> Json.arr()))
    storedVariables mustBe Seq.empty
  }

}
