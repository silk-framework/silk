package controllers.transform


import helper.IntegrationTestTrait
import org.scalatestplus.play.PlaySpec
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WS

/**
  * Base trait for transformation API tests.
  */
trait TransformTaskApiTestBase extends PlaySpec with IntegrationTestTrait {

  protected def log: Logger = Logger(getClass.getName)

  def printResponses: Boolean

  protected val project = "TransformTestProject"
  protected val task = "TestTransform"

  override def workspaceProvider = "inMemory"

  def jsonGetRequest(url: String): JsValue = {
    var request = WS.url(url)
    request = request.withHeaders("Accept" -> "application/json")
    val response = request.get()
    val json = checkResponse(response).json

    if(printResponses) {
      println(s"Get request on $url resulting in:\n" + Json.prettyPrint(json))
    }

    json
  }

}
