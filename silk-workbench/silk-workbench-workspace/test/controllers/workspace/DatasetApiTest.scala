package controllers.workspace

import helper.IntegrationTestTrait
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json

class DatasetApiTest extends PlaySpec with IntegrationTestTrait {

  private val project = "project"

  override def workspaceProvider: String = "inMemory"

  protected override def routes = Some(classOf[test.Routes])

  "setup" in {
    createProject(project)
  }

  "add datasets using XML" in {
    val dataset = "dataset1"
    var request = client.url(s"$baseUrl/workspace/projects/$project/datasets/dataset1")
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
    request = request.withHeaders("Accept" -> "application/json")
    val response = request.put(
      Json.obj(
        "taskType" -> "Dataset",
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
    request = request.withHeaders("Accept" -> "application/json")
    val response = checkResponse(request.get())
    response.json mustBe
      Json.obj(
        "id" -> dataset,
        "project" -> project,
        "metadata" ->
          Json.obj(
            "label" -> "label 1",
            "description" -> "description 1",
            "modified" -> "2018-03-08T15:01:06.609Z"
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

  "get dataset using XML" in {
    val dataset = "dataset2"
    var request = client.url(s"$baseUrl/workspace/projects/$project/datasets/$dataset")
    request = request.withHeaders("Accept" -> "application/xml")
    val response = checkResponse(request.get())
    val xml = response.xml

    (xml \ "@id").text mustBe dataset
    (xml \ "@uriProperty").text mustBe "URI"
    (xml \ "MetaData" \ "Label").text mustBe "label 2"
    (xml \ "MetaData" \ "Description").text mustBe "description 2"
    (xml \ "Param").text mustBe "urn:dataset2"
  }

}
