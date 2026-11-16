package org.silkframework.workbench.logging

/**
  * A page of log lines, oldest first.
  *
  * @param lines      The matching lines.
  * @param nextCursor Highest sequence examined. A polling client passes it back as its next 'since' value. It is the
  *                   last examined rather than the last returned sequence, so filtered-out lines are not examined again
  *                   while lines beyond a truncated page are not skipped.
  * @param truncated  True, if the limit was reached before the end of the store, i.e. more lines can be fetched now.
  */
case class LogPage(lines: Seq[LogLine], nextCursor: Long, truncated: Boolean)
