package controllers.workspaceApi.coreApi.logApi

import ch.qos.logback.classic.Level
import org.silkframework.runtime.validation.BadUserInputException
import org.silkframework.workbench.logging.{LogBufferConfig, LogLine}

import java.util.Locale

/**
  * A validated set of log filters.
  *
  * @param minLevel       Minimum level a line has to be logged at.
  * @param loggerPrefixes Logger name prefixes, of which one has to match.
  * @param contains       Lower-cased substrings, all of which have to appear in the message or the rendered exception.
  * @param limit          Maximum number of lines to return, already clamped.
  */
case class LogQuery(minLevel: Level,
                    loggerPrefixes: Seq[String],
                    contains: Seq[String],
                    limit: Int) {

  def matches(line: LogLine): Boolean = {
    matchesLevel(line) && matchesLogger(line) && matchesContains(line)
  }

  private def matchesLevel(line: LogLine): Boolean = {
    Level.toLevel(line.level, Level.TRACE).isGreaterOrEqual(minLevel)
  }

  private def matchesLogger(line: LogLine): Boolean = {
    loggerPrefixes.isEmpty || loggerPrefixes.exists(line.logger.startsWith)
  }

  private def matchesContains(line: LogLine): Boolean = {
    contains.isEmpty || {
      val text = (line.message + "\n" + line.throwable.getOrElse("")).toLowerCase(Locale.ROOT)
      contains.forall(text.contains)
    }
  }
}

object LogQuery {

  /** Lines returned when a request does not ask for a limit. */
  final val defaultLimit = 200

  /** Highest limit a request may ask for. */
  final val maxLimit = 2000

  /**
    * Validates and normalises the raw request parameters.
    * The limit is clamped rather than rejected, so a client asking for more than allowed still gets an answer.
    */
  def apply(level: Option[String], logger: Seq[String], contains: Seq[String], limit: Option[Int]): LogQuery = {
    LogQuery(
      minLevel = parseLevel(level),
      loggerPrefixes = clean(logger),
      contains = clean(contains).map(_.toLowerCase(Locale.ROOT)),
      limit = limit.map(l => math.max(1, math.min(l, maxLimit))).getOrElse(defaultLimit)
    )
  }

  private def parseLevel(level: Option[String]): Level = {
    level.map(_.trim).filter(_.nonEmpty) match {
      case None =>
        Level.TRACE
      case Some(name) =>
        val upper = name.toUpperCase(Locale.ROOT)
        if (!LogBufferConfig.validLevels.contains(upper)) {
          throw BadUserInputException(s"Unknown log level '$name', expected one of ${LogBufferConfig.validLevels.mkString(", ")}")
        }
        Level.toLevel(upper)
    }
  }

  private def clean(values: Seq[String]): Seq[String] = values.map(_.trim).filter(_.nonEmpty)
}
