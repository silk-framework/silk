package org.silkframework.workspace.activity.workflow

import java.util.{Date, GregorianCalendar}

/**
  * Statistics of an activity run.
  */
case class ActivityExecutionStatistics(startedByUser: String,
                                       startedAt: GregorianCalendar,
                                       finishedAt: GregorianCalendar,
                                       cancelledAt: Option[GregorianCalendar],
                                       cancelledBy: Option[String])
