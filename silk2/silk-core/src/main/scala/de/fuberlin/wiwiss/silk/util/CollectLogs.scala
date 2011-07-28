package de.fuberlin.wiwiss.silk.util

import java.util.logging.{Handler, LogRecord, Logger, Level}

/**
 * Collects all log message which occur in a specific scope.
 */
object CollectLogs {
  def apply(level: Level = Level.WARNING, namespace: String = "de.fuberlin.wiwiss.silk")(f: => Unit): Seq[LogRecord] = {
    val logCollector = new LogCollector
    logCollector.setLevel(level)

    Logger.getLogger(namespace).addHandler(logCollector)

    f

    Logger.getLogger(namespace).removeHandler(logCollector)

    logCollector.records
  }

  private class LogCollector extends Handler {
    var records = List[LogRecord]()

    def publish(record: LogRecord) {
      if (isLoggable(record)) {
        records ::= record
      }
    }

    def flush() {

    }

    def close() {

    }
  }

}