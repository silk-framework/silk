package org.silkframework.workspace.changes

import org.silkframework.runtime.activity.UserContext

/** Lets API tests record outcome changes, e.g. a workflow run, which only the write path inside the workspace package can. */
object TestJournalAccess {

  def record(journal: ChangeJournal, change: Change)(implicit userContext: UserContext): Unit = {
    journal.record(change)
  }
}
