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
case class ActivityExecutionMetaData(startedByUser: Option[User] = None,
                                     startedAt: Option[Long] = None,
                                     finishedAt: Option[Long] = None,
                                     cancelledAt: Option[Long] = None,
                                     cancelledBy: Option[User] = None,
                                     finishStatus: Option[Status] = None)
