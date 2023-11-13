package helper

import play.api.Application
import play.api.libs.json.{JsValue, Json, Reads, Writes}
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}
import play.api.mvc.Call

import java.util.logging.Logger
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

trait ApiClient {

  protected val log: Logger = Logger.getLogger(getClass.getName)

  def baseUrl: String

  def port: Int

  implicit def app: Application

  implicit lazy val client: WSClient = app.injector.instanceOf[WSClient]

  protected def createRequest(call: Call): WSRequest = {
    client.url(baseUrl + call.url)
  }

  /**
    * Simple JSON GET request.
    */
  protected def getRequest[ResponseType](call: Call)
                                        (implicit reads: Reads[ResponseType]): ResponseType = {
    val request = createRequest(call)
    val response = request.get()
    val responseJson = checkResponse(response).body[JsValue]
    Json.fromJson[ResponseType](responseJson).get
  }

  /**
    * Simple JSON PUT request.
    */
  protected def putRequest[RequestType, ResponseType](call: Call, requestBody: RequestType)
                                                      (implicit writes: Writes[RequestType], reads: Reads[ResponseType]): ResponseType = {
    val request = createRequest(call)
    val response = request.put(Json.toJson(requestBody))
    val responseJson = checkResponse(response).body[JsValue]
    Json.fromJson[ResponseType](responseJson).get
  }

  /**
    * Simple JSON POST request.
    */
  protected def postRequest[RequestType, ResponseType](call: Call, requestBody: RequestType)
                                                      (implicit writes: Writes[RequestType], reads: Reads[ResponseType]): ResponseType = {
    val request = createRequest(call)
    val response = request.post(Json.toJson(requestBody))
    val responseJson = checkResponse(response).body[JsValue]
    Json.fromJson[ResponseType](responseJson).get
  }

  /**
    * Simple delete request.
    */
  protected def deleteRequest(call: Call): Unit = {
    val request = createRequest(call)
    val response = request.delete()
    checkResponse(response)
  }

  protected def checkResponse(futureResponse: Future[WSResponse],
                    responseCodePrefix: Char = '2'): WSResponse = {
    val response = Await.result(futureResponse, 200.seconds)
    if(response.status.toString.head != responseCodePrefix) {
      throw new RequestFailedException(s"Expected status: ${responseCodePrefix}XX, received status: ${response.status}. Response Body: ${response.body}", response)
    }
    response
  }

  protected def checkResponseExactStatusCode(futureResponse: Future[WSResponse],
                                             responseCode: Int = 200): WSResponse = {
    val response = Await.result(futureResponse, 200.seconds)
    if(response.status != responseCode) {
      throw new RequestFailedException(s"Expected status: $responseCode, received status: ${response.status}. Response Body: ${response.body}", response)
    }
    response
  }
}

class RequestFailedException(message: String, val response: WSResponse) extends Exception(message)
