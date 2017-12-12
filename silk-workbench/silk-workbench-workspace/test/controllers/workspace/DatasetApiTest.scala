package controllers.workspace

import helper.IntegrationTestTrait
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.libs.ws.WS

class DatasetApiTest extends PlaySpec with IntegrationTestTrait {

  private val project = "project"

  override def workspaceProvider: String = "inMemory"

  protected override def routes = Some("test.Routes")

  "setup" in {
    createProject(project)
  }

  "add datasets using XML" in {
    val dataset = "dataset1"
    var request = WS.url(s"$baseUrl/workspace/projects/$project/datasets/dataset1")
    val response = request.put(
      <Dataset id={dataset} type="internal">
        <MetaData>
          <Label>label 1</Label>
          <Description>description 1</Description>
        </MetaData>
        <Param name="graphUri" value="urn:dataset1"/>
      </Dataset>
    )
    checkResponse(response)
  }

  "add datasets using JSON" in {
    val dataset = "dataset2"
    var request = WS.url(s"$baseUrl/workspace/projects/$project/datasets/$dataset")
    request = request.withHeaders("Accept" -> "application/json")
    val response = request.put(
      Json.obj(
        "id" -> dataset,
        "metadata" ->
          Json.obj(
            "label" -> "label 2",
            "description" -> "description 2"
          ),
        "type" -> "internal",
        "parameters" ->
          Json.obj(
            "graphUri" -> "urn:dataset2"
          )
      )
    )
    checkResponse(response)
  }

  "get dataset using JSON" in {
    val dataset = "dataset1"
    var request = WS.url(s"$baseUrl/workspace/projects/$project/datasets/$dataset")
    request = request.withHeaders("Accept" -> "application/json")
    val response = checkResponse(request.get())
    response.json mustBe
      Json.obj(
        "id" -> dataset,
        "metadata" ->
          Json.obj(
            "label" -> "label 1",
            "description" -> "description 1"
          ),
        "type" -> "internal",
        "parameters" -> Json.obj(
          "graphUri" -> "urn:dataset1"
        )
      )
  }

  "get dataset using XML" in {
    val dataset = "dataset2"
    var request = WS.url(s"$baseUrl/workspace/projects/$project/datasets/$dataset")
    request = request.withHeaders("Accept" -> "application/xml")
    val response = checkResponse(request.get())
    val xml = response.xml

    (xml \ "@id").text mustBe dataset
    (xml \ "MetaData" \ "Label").text mustBe "label 2"
    (xml \ "MetaData" \ "Description").text mustBe "description 2"
    (xml \ "Param").text mustBe "urn:dataset2"
  }

}
