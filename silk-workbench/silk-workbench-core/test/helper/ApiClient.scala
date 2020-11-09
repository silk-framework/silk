package helper

import play.api.Application
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}
import play.api.mvc.Call

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

trait ApiClient {

  def baseUrl: String

  def port: Int

  implicit def app: Application

  implicit lazy val client: WSClient = app.injector.instanceOf[WSClient]

  protected def createRequest(call: Call): WSRequest = {
    client.url(baseUrl + call.url)
  }

  protected def checkResponse(futureResponse: Future[WSResponse],
                    responseCodePrefix: Char = '2'): WSResponse = {
    val response = Await.result(futureResponse, 200.seconds)
    assert(response.status.toString.head == responseCodePrefix, s"Expected status: ${responseCodePrefix + "XX"}, received status: ${response.status}. Response Body: ${response.body}")
    response
  }

  protected def checkResponseExactStatusCode(futureResponse: Future[WSResponse],
                                             responseCode: Int = 200): WSResponse = {
    val response = Await.result(futureResponse, 200.seconds)
    assert(response.status == responseCode, s"Expected status: $responseCode, received status: ${response.status}. Response Body: ${response.body}")
    response
  }
}
