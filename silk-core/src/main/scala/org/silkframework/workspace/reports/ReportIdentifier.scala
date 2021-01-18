package org.silkframework.workspace.reports

import java.time.Instant

import org.silkframework.util.Identifier

/**
  * Identifies a report.
  *
  * A report is identified by its project ID, its tasks ID and the time it has been created.
  */
case class ReportIdentifier(projectId: Identifier, taskId: Identifier, time: Instant)

object ReportIdentifier {

  def create(projectId: Identifier, taskId: Identifier): ReportIdentifier = {
    ReportIdentifier(projectId, taskId, Instant.now)
  }

}
