package org.silkframework.workspace.changes

import org.silkframework.runtime.activity.UserContext
import org.silkframework.workspace.Project

/**
  * The placeholder for a stored entry whose change cannot be read, e.g. because its plugin is gone or its format
  * changed: the entry stays listed with its type and summary, so the seqs, the links between entries and the
  * unreviewed count stay intact, but it cannot be reverted.
  */
case class UnreadableChange(override val changeType: String, summary: String) extends Change {

  override def inverse: Option[Change] = None

  override def applyTo(project: Project)(implicit userContext: UserContext): Unit = throw new IllegalStateException(notRead)

  override def conflict(context: ConflictContext)(implicit userContext: UserContext): Option[String] = Some(notRead)

  private def notRead: String = s"The stored change ($changeType) could not be read and cannot be applied: $summary"
}
