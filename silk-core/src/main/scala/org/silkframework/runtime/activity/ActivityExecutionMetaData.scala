package org.silkframework.runtime.activity

import java.time.Instant

import org.silkframework.runtime.users.User

/**
  * Stores information about the activity execution.
  * @param startedByUser The user who started the activity. This user may not be known, depending on the user manager that is running.
  * @param queuedAt      The timestamp when the activity has been added to the waiting queue. This is None, if the activity has never been queued.
  * @param startedAt     The timestamp of when the activity has been started. This is None if the activity has never been started.
  * @param finishedAt    The timestamp of when the activity
  * @param cancelledAt
  * @param cancelledBy
  */
case class ActivityExecutionMetaData(startedByUser: Option[User] = None,
                                     queuedAt: Option[Instant] = None,
                                     startedAt: Option[Instant] = None,
                                     finishedAt: Option[Instant] = None,
                                     cancelledAt: Option[Instant] = None,
                                     cancelledBy: Option[User] = None,
                                     finishStatus: Option[Status] = None)
