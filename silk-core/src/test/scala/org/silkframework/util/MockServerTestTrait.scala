package org.silkframework.util

import java.net.{BindException, InetSocketAddress}

import com.sun.net.httpserver.{HttpExchange, HttpHandler, HttpServer}

import scala.util.{Failure, Success, Try}

/**
  * Test trait that adds server mocking helper methods.
  */
trait MockServerTestTrait {
  // The start port where to look for open ports to start the mock server
  final val START_PORT = 10600

  final val OK = 200
  final val INTERNAL_SERVER_ERROR_CODE = 500
  final val BAD_REQUEST_ERROR_CODE = 400
  final val UNAUTHORIZED = 401
  final val FORBIDDEN = 403
  var servers: List[HttpServer] = List.empty

  // From https://stackoverflow.com/questions/3732109/simple-http-server-in-java-using-only-java-se-api
  def withAdditionalServer(servedContent: Traversable[ServedContent])(withPort: Int => Unit): Unit = {
    val port = startServer(servedContent)
    withPort(port)
  }

  /** Starts a server that delivers the specified content and returns the port the server runs on. */
  def startServer(servedContents: Traversable[ContentHandler]): Int = {
    val server: HttpServer = createHttpServer
    for (servedContent <- servedContents) {
      val handler = new HttpHandler {
        override def handle(httpExchange: HttpExchange): Unit = {
          val responseContent = servedContent match {
            case s: ServedContent =>
              s
            case DynamicContent(_, contentFN) =>
              contentFN()
          }
          val response = responseContent.content
          val responseHeaders = httpExchange.getResponseHeaders
          responseHeaders.add("content-type", responseContent.contentType)
          httpExchange.sendResponseHeaders(responseContent.statusCode, response.getBytes("UTF-8").length)
          val os = httpExchange.getResponseBody
          os.write(response.getBytes("UTF-8"))
          os.close()
        }
      }
      server.createContext(servedContent.contextPath, handler)
    }
    server.setExecutor(null) // creates a default executor
    server.start()
    servers ::= server
    server.getAddress.getPort
  }

  def stopAllRegisteredServers(): Unit = {
    for(server <- servers) {
      Try(server.stop(0))
    }
  }

  private def createHttpServer: HttpServer = {
    var port = START_PORT
    var serverOpt: Option[HttpServer] = None
    while (serverOpt.isEmpty) {
      Try(HttpServer.create(new InetSocketAddress(port), 0)) match {
        case Success(s) =>
          serverOpt = Some(s)
        case Failure(e: BindException) =>
          port += 1
        case Failure(e) =>
          throw e
      }
    }
    serverOpt.get
  }
}

sealed trait ContentHandler {
  def contextPath: String
}

/**
  * Static content that is served by the mock server.
  * @param contextPath The context path of the endpoint serving the content.
  * @param content     The text content that should be returned.
  * @param contentType The content type of the response.
  * @param statusCode  The status code of the response.
  */
case class ServedContent(contextPath: String = "/",
                         content: String = "",
                         contentType: String = "text/plain",
                         statusCode: Int = 200) extends ContentHandler

case class DynamicContent(contextPath: String = "/", content: () => ServedContent) extends ContentHandler