package org.silkframework.workspace.reports

import java.time.Instant

import org.silkframework.util.Identifier

case class ReportMetaData(projectId: Identifier, taskId: Identifier, time: Instant)

