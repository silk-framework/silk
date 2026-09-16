package controllers.workspaceApi.coreApi.logApi

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.silkframework.runtime.validation.BadUserInputException
import org.silkframework.workbench.logging.LogLine

class LogQueryTest extends AnyFlatSpec with Matchers {

  behavior of "LogQuery"

  private def query(level: Option[String] = None,
                    logger: Seq[String] = Seq.empty,
                    contains: Seq[String] = Seq.empty,
                    limit: Option[Int] = None): LogQuery = {
    LogQuery(level, logger, contains, limit)
  }

  private def line(level: String = "INFO",
                   logger: String = "org.silkframework.a",
                   message: String = "message",
                   throwable: Option[String] = None): LogLine = {
    LogLine(0, 0, level, logger, None, message, throwable)
  }

  it should "match everything without filters" in {
    query().matches(line()) mustBe true
  }

  it should "filter by minimum level, ignoring case" in {
    val warnings = query(level = Some("warn"))
    warnings.matches(line("INFO")) mustBe false
    warnings.matches(line("WARN")) mustBe true
    warnings.matches(line("ERROR")) mustBe true
  }

  it should "reject an unknown level" in {
    a[BadUserInputException] must be thrownBy query(level = Some("LOUD"))
    a[BadUserInputException] must be thrownBy query(level = Some("OFF"))
  }

  it should "filter by logger prefixes" in {
    val prefixes = query(logger = Seq("org.silkframework", "com.eccenca"))
    prefixes.matches(line(logger = "org.silkframework.workspace")) mustBe true
    prefixes.matches(line(logger = "com.eccenca.di")) mustBe true
    prefixes.matches(line(logger = "play.api")) mustBe false
  }

  it should "require all contains terms in the message or exception, ignoring case" in {
    val terms = query(contains = Seq("Failed", "project X"))
    terms.matches(line(message = "failed to load PROJECT x")) mustBe true
    terms.matches(line(message = "failed", throwable = Some("Exception: Project X missing"))) mustBe true
    terms.matches(line(message = "failed")) mustBe false
  }

  it should "ignore blank contains terms" in {
    query(contains = Seq(" a ", "", "  ")).contains mustBe Seq("a")
  }

  it should "clamp the limit and use the default if none is given" in {
    query().limit mustBe 200
    query(limit = Some(5000)).limit mustBe 2000
    query(limit = Some(0)).limit mustBe 1
    query(limit = Some(42)).limit mustBe 42
  }
}
