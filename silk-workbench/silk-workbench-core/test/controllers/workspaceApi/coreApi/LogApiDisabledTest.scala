package controllers.workspaceApi.coreApi

import controllers.workspaceApi.coreApi.logApi.LogBufferStatus
import helper.IntegrationTestTrait
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

/** The log buffer is disabled by default. */
class LogApiDisabledTest extends AnyFlatSpec with IntegrationTestTrait with Matchers {

  behavior of "Log API while disabled"

  override def workspaceProviderId = "inMemoryWorkspaceProvider"

  protected override def routes = Some(classOf[test.Routes])

  it should "answer with 503 and report the buffer as disabled" in {
    checkResponseExactStatusCode(client.url(s"$baseUrl/api/core/system/logs").get(), SERVICE_UNAVAILABLE)
    val status = checkResponse(client.url(s"$baseUrl/api/core/system/logs/status").get()).json.as[LogBufferStatus]
    status.enabled mustBe false
    status.attached mustBe false
    status.size mustBe 0
    status.lastSequence mustBe -1
  }
}
