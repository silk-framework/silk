package config

import com.typesafe.config.ConfigFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import play.api.mvc.RequestHeader
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

  private def hostRequest(host: String): RequestHeader = {
    FakeRequest("GET", "/").withHeaders("Host" -> host)
  }
}
