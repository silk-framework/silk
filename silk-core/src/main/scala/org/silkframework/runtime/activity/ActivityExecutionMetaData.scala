package org.silkframework.runtime.activity

import org.silkframework.runtime.users.User

/**
  * Stores information about the activity execution.
  * @param startedByUser The user who started the activity. This user may not be known, depending on the user manager that is running.
  * @param startedAt     The timestamp of when the activity has been started. This is None if the activity has never been started.
  * @param finishedAt    The timestamp of when the activity
  * @param cancelledAt
  * @param cancelledBy
  */
case class ActivityExecutionMetaData(startedByUser: Option[User],
                                     startedAt: Option[Long],
                                     finishedAt: Option[Long],
                                     cancelledAt: Option[Long],
                                     cancelledBy: Option[User],
                                     finishStatus: Option[Status])
