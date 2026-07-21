package config

import com.typesafe.config.ConfigFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import play.api.mvc.request.RemoteConnection
import play.api.mvc.{RequestHeader, WrappedRequest}
import play.api.test.FakeRequest

class WorkbenchConfigTest extends AnyFlatSpec with Matchers {
  behavior of "WorkbenchConfig URL helpers"

  it should "use the configured http.port as localhost fallback for the public base URL" in {
    val config = ConfigFactory.parseString(
      """http.port = 4711
        |workbench.protocol = "http"
        |""".stripMargin
    )

    WorkbenchConfig.publicBaseUrl(config, request = None, includeApplicationContext = false) mustBe "http://localhost:4711"
  }

  it should "include the application context when requested for a request-based fallback" in {
    val config = ConfigFactory.parseString(
      """workbench.protocol = "https"
        |play.http.context = "/dataintegration"
        |""".stripMargin
    )
    val request = hostRequest("example.org:8088")

    WorkbenchConfig.publicBaseUrl(config, request = Some(request)) mustBe "https://example.org:8088/dataintegration"
  }

  it should "omit the application context when requested for a request-based fallback" in {
    val config = ConfigFactory.parseString(
      """workbench.protocol = "https"
        |play.http.context = "/dataintegration"
        |""".stripMargin
    )
    val request = hostRequest("example.org:8088")

    WorkbenchConfig.publicBaseUrl(config, request = Some(request), includeApplicationContext = false) mustBe "https://example.org:8088"
  }

  it should "prefer the configured public host over a request host fallback" in {
    val config = ConfigFactory.parseString(
      """workbench.protocol = "https"
        |workbench.host = "configured.example:9443"
        |play.http.context = "/dataintegration"
        |""".stripMargin
    )
    val request = hostRequest("request.example:8088")

    WorkbenchConfig.publicBaseUrl(config, request = Some(request)) mustBe "https://configured.example:9443/dataintegration"
  }

  it should "fall back to the request scheme when workbench.protocol is not configured" in {
    val config = ConfigFactory.parseString("play.http.context = \"/dataintegration\"")

    WorkbenchConfig.publicBaseUrl(config, request = Some(hostRequest("example.org:8088", secure = true))) mustBe
      "https://example.org:8088/dataintegration"
    WorkbenchConfig.publicBaseUrl(config, request = Some(hostRequest("example.org:8088", secure = false))) mustBe
      "http://example.org:8088/dataintegration"
  }

  it should "let a configured workbench.protocol override the request scheme" in {
    val config = ConfigFactory.parseString("workbench.protocol = \"http\"")

    // Even a secure request keeps http when the protocol is pinned by configuration.
    WorkbenchConfig.publicBaseUrl(config, request = Some(hostRequest("example.org:8088", secure = true)),
      includeApplicationContext = false) mustBe "http://example.org:8088"
  }

  private def hostRequest(host: String, secure: Boolean = false): RequestHeader = {
    val request = FakeRequest("GET", "/").withHeaders("Host" -> host)
    // FakeRequest has no scheme setter and RequestHeader.secure is final (derived from the
    // connection), so wrap it with a secure RemoteConnection when needed.
    if (secure) {
      new WrappedRequest(request) {
        override val connection: RemoteConnection = RemoteConnection("127.0.0.1", secure = true, None)
      }
    } else {
      request
    }
  }
}
