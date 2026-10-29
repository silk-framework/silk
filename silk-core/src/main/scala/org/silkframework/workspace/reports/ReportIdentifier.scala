package org.silkframework.workspace.reports

import java.time.Instant
import java.time.temporal.ChronoUnit

import org.silkframework.util.Identifier

/**
  * Identifies a report.
  *
  * A report is identified by its project ID, its tasks ID and the time it has been created.
  */
case class ReportIdentifier(projectId: Identifier, taskId: Identifier, time: Instant)

object ReportIdentifier {

  def create(projectId: Identifier, taskId: Identifier): ReportIdentifier = {
    // Millisecond precision: report stores encode the time at millisecond granularity (e.g. in file
    // names), so a finer-grained identifier would not survive the round trip through the store.
    ReportIdentifier(projectId, taskId, Instant.now.truncatedTo(ChronoUnit.MILLIS))
  }

}
