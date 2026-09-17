package controllers.workspaceApi.coreApi

import controllers.workspaceApi.coreApi.logApi.{LogBufferStatus, LogTailResponse}
import helper.IntegrationTestTrait
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.silkframework.util.ConfigTestTrait
import org.silkframework.workbench.logging.LogBuffer
import org.slf4j.LoggerFactory
import play.api.libs.ws.WSResponse

import java.time.Instant
import scala.concurrent.Future

class LogApiTest extends AnyFlatSpec with IntegrationTestTrait with Matchers with ConfigTestTrait {

  behavior of "Log API"

  override def workspaceProviderId = "inMemoryWorkspaceProvider"

  protected override def routes = Some(classOf[test.Routes])

  override def propertyMap: Map[String, Option[String]] = Map(
    "logging.buffer.enabled" -> Some("true"),
    "logging.buffer.capacity" -> Some("20"),
    "logging.buffer.level" -> Some("INFO")
  )

  private val slf4jLog = LoggerFactory.getLogger("org.silkframework.logApiTest")

  private val julLog = java.util.logging.Logger.getLogger("org.silkframework.logApiTest.jul")

  private def logsUrl = s"$baseUrl/api/core/system/logs"

  private def get(query: String = ""): Future[WSResponse] = client.url(logsUrl + query).get()

  private def tail(query: String = ""): LogTailResponse = checkResponse(get(query)).json.as[LogTailResponse]

  it should "report the buffer as enabled and attached" in {
    val bufferStatus = checkResponse(client.url(logsUrl + "/status").get()).json.as[LogBufferStatus]
    bufferStatus.enabled mustBe true
    bufferStatus.attached mustBe true
    bufferStatus.capacity mustBe 20
    bufferStatus.captureLevel mustBe "INFO"
    bufferStatus.instanceId must not be empty
  }

  it should "return lines logged through SLF4J and java.util.logging" in {
    slf4jLog.warn("slf4j marker-A")
    julLog.warning("jul marker-A")
    val lines = tail("?contains=marker-A").lines
    lines.map(_.message) mustBe Seq("slf4j marker-A", "jul marker-A")
    lines.map(_.level) mustBe Seq("WARN", "WARN")
    lines.map(_.logger) mustBe Seq("org.silkframework.logApiTest", "org.silkframework.logApiTest.jul")
    lines.map(_.sequence) mustBe sorted
    lines.foreach(line => noException must be thrownBy Instant.parse(line.timestamp))
  }

  it should "filter by level and logger" in {
    slf4jLog.info("marker-B info")
    slf4jLog.warn("marker-B warn")
    tail("?contains=marker-B&level=WARN").lines.map(_.message) mustBe Seq("marker-B warn")
    tail("?contains=marker-B&logger=org.silkframework.logApiTest.jul").lines mustBe empty
    tail("?contains=marker-B&logger=org.silkframework.logApiTest").lines.size mustBe 2
  }

  it should "poll for new lines with since" in {
    val current = tail()
    slf4jLog.warn("marker-C")
    val next = tail(s"?since=${current.lastSequence}")
    next.lines.map(_.message) must contain("marker-C")
    next.lines.forall(_.sequence > current.lastSequence) mustBe true
    next.lastSequence must be > current.lastSequence
    next.dropped mustBe 0
    tail(s"?since=${next.lastSequence}&contains=marker-C").lines mustBe empty
  }

  it should "report an instance id with a per-start suffix" in {
    val instanceId = tail().instanceId
    instanceId must fullyMatch regex ".+-\\d+"
    instanceId must startWith(LogBuffer.hostName + "-")
    checkResponse(client.url(logsUrl + "/status").get()).json.as[LogBufferStatus].instanceId mustBe instanceId
  }

  it should "keep the newest lines when the limit cuts the result" in {
    for (i <- 1 to 5) slf4jLog.warn(s"marker-D $i")
    val page = tail("?contains=marker-D&limit=2")
    page.lines.map(_.message) mustBe Seq("marker-D 4", "marker-D 5")
    page.truncated mustBe true
  }

  it should "report dropped lines when the client fell behind" in {
    for (i <- 1 to 30) slf4jLog.warn(s"flood $i")
    val page = tail("?since=0")
    page.firstSequence must be > 0L
    page.dropped must be > 0L
    // -1 is the cursor an empty buffer hands out, so everything before firstSequence counts as dropped
    val fromStart = tail("?since=-1")
    fromStart.dropped mustBe fromStart.firstSequence
  }

  it should "reject an invalid level" in {
    checkResponseExactStatusCode(get("?level=LOUD"), BAD_REQUEST)
  }
}
