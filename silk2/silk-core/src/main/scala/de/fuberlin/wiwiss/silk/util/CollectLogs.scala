package de.fuberlin.wiwiss.silk.util

import java.util.logging.{Handler, LogRecord, Logger, Level}

object CollectLogs
{
  def apply(level : Level = Level.WARNING)(f : => Unit) : Seq[LogRecord] =
  {
    val logCollector = new LogCollector
    logCollector.setLevel(level)

    Logger.getLogger("de.fuberlin.wiwiss.silk").addHandler(logCollector)

    f

    Logger.getLogger("de.fuberlin.wiwiss.silk").removeHandler(logCollector)

    logCollector.records
  }

  private class LogCollector extends Handler
  {
    var records = List[LogRecord]()

    def publish(record: LogRecord)
    {
      if(isLoggable(record))
      {
         records ::= record
      }
    }

    def flush()
    {

    }

    def close()
    {

    }
  }
}