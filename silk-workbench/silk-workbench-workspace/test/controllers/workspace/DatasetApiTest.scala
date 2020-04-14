package controllers.workspace

import helper.IntegrationTestTrait
import org.scalatestplus.play.PlaySpec
import org.silkframework.serialization.json.JsonSerializers
import play.api.libs.json.{JsObject, Json}

class DatasetApiTest extends PlaySpec with IntegrationTestTrait {

  private val project = "project"

  override def workspaceProviderId: String = "inMemory"

  protected override def routes = Some(classOf[test.Routes])

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
        "taskType" -> JsonSerializers.TASK_TYPE_DATASET,
        "id" -> dataset,
        "metadata" ->
          Json.obj(
            "label" -> "label 2",
            "description" -> "description 2"
          ),
        "data" -> Json.obj(
          "uriProperty" -> "URI",
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
    (json \ "taskType").as[String] mustBe "Dataset"
    (json \ "data").as[JsObject] mustBe Json.obj(
      "taskType" -> "Dataset",
      "type" -> "internal",
      "parameters" -> Json.obj(
        "graphUri" -> "urn:dataset1"
      )
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

}
