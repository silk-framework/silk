package helper

import play.api.Application
import play.api.libs.ws.{WSClient, WSResponse}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

trait ApiClient {

  def baseUrl: String

  def port: Int

  implicit def app: Application

  implicit lazy val client: WSClient = app.injector.instanceOf[WSClient]

  protected def checkResponse(futureResponse: Future[WSResponse],
                    responseCodePrefix: Char = '2'): WSResponse = {
    val response = Await.result(futureResponse, 200.seconds)
    assert(response.status.toString.head + "xx" == responseCodePrefix + "xx", s"Status text: ${response.statusText}. Response Body: ${response.body}")
    response
  }
  
}
