/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.silkframework.util

import java.util.logging.{Handler, Level, LogRecord, Logger}

/**
 * Collects all log message which occur in a specific scope.
 */
object CollectLogs {
  def apply(level: Level = Level.WARNING, namespace: String = "org.silkframework")(f: => Unit): Seq[LogRecord] = {
    val logCollector = new LogCollector
    logCollector.setLevel(level)

    Logger.getLogger(namespace).addHandler(logCollector)

    f

    Logger.getLogger(namespace).removeHandler(logCollector)

    logCollector.records
  }

  private class LogCollector extends Handler {
    var records: List[LogRecord] = List[LogRecord]()

    def publish(record: LogRecord): Unit = {
      if (isLoggable(record)) {
        records ::= record
      }
    }

    def flush(): Unit = {

    }

    def close(): Unit = {

    }
  }

}