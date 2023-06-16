package org.silkframework.util

import java.io.BufferedInputStream
import java.net.{BindException, InetSocketAddress}

import com.sun.net.httpserver.{HttpExchange, HttpHandler, HttpServer}

import scala.util.{Failure, Success, Try}

/**
  * Test trait that adds server mocking helper methods.
  */
trait MockServerTestTrait extends StatusCodeTestTrait {
  // The start port where to look for open ports to start the mock server
  final val START_PORT = 10600
  @volatile
  var servers: List[HttpServer] = List.empty

  // From https://stackoverflow.com/questions/3732109/simple-http-server-in-java-using-only-java-se-api
  def withAdditionalServer(servedContent: Iterable[ContentHandler])(withPort: Int => Unit): Unit = {
    val server = startServerWithContent(servedContent)
    try {
      withPort(server.getAddress.getPort)
    } finally {
      server.stop(0)
    }
  }

  /** Starts a server that delivers the specified content and returns the port the server runs on. */
  def startServer(servedContents: Iterable[ContentHandler]): Int = {
    val server = startServerWithContent(servedContents)
    servers ::= server
    server.getAddress.getPort
  }

  private def startServerWithContent(servedContents: Iterable[ContentHandler]): HttpServer = {
    val server: HttpServer = createHttpServer
    for (servedContent <- servedContents) {
      val handler = new HttpHandler {
        override def handle(httpExchange: HttpExchange): Unit = {
          servedContent match {
            case s: ServedContent =>
              respond(httpExchange, s)
            case DynamicContent(_, contentFN) =>
              respond(httpExchange, contentFN(httpExchange))
            case FullControl(_, handleExchange) =>
              handleExchange(httpExchange)
          }
        }
      }
      server.createContext(servedContent.contextPath, handler)
    }
    server.setExecutor(null) // creates a default executor
    server.start()
    server
  }

  protected def respond(httpExchange: HttpExchange, responseContent: ServedContent): Unit = {
    // Consume body if available
    val is = new BufferedInputStream(httpExchange.getRequestBody)
    while(is.read() != -1) {}
    val responseOpt = responseContent.content
    val responseHeaders = httpExchange.getResponseHeaders
    if(responseContent.statusCode == 204) {
      // No Content
      httpExchange.sendResponseHeaders(responseContent.statusCode, -1)
    } else {
      responseOpt match {
        case Some(response) =>
          responseHeaders.add("content-type", responseContent.contentType)
          httpExchange.sendResponseHeaders(responseContent.statusCode, response.getBytes("UTF-8").length)
          val os = httpExchange.getResponseBody
          os.write(response.getBytes("UTF-8"))
          os.close()
        case None =>
          httpExchange.sendResponseHeaders(responseContent.statusCode, -1)
      }
    }
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
  * @param content     The text content that should be returned. None for no body.
  * @param contentType The content type of the response.
  * @param statusCode  The status code of the response.
  */
case class ServedContent(contextPath: String = "/",
                         content: Option[String] = None,
                         contentType: String = "text/plain",
                         statusCode: Int = 200) extends ContentHandler

/**
  * Serve dynamic content that changes over time
  * @param contextPath The context path of the endpoint
  * @param contentFn   The content function that returns the served content
  */
case class DynamicContent(contextPath: String = "/",
                          contentFn: HttpExchange => ServedContent) extends ContentHandler

/**
  * Gives full control over the HTTP Exchange.
  * @param contextPath     The context path of the endpoint
  * @param handleRequestFn Handle the HTTP exchange, i.e. send data via output stream and close it, etc.
  */
case class FullControl(contextPath: String = "/",
                       handleRequestFn: HttpExchange => Unit) extends ContentHandler
