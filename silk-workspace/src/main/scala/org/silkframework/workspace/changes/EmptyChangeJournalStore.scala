package org.silkframework.workspace.changes

import org.silkframework.runtime.plugin.annotations.Plugin
import org.silkframework.util.Identifier

@Plugin(
  id = "emptyChangeJournal",
  label = "None",
  description = "Discards changes: nothing is recorded and no change can be reverted."
)
case class EmptyChangeJournalStore() extends ChangeJournalStore {

  override def keepsEntries: Boolean = false

  override def append(project: Identifier, entry: ChangeEntry): Unit = { }

  override def entries(project: Identifier): Seq[ChangeEntry] = Seq.empty

  override def reviewedUpTo(project: Identifier): Int = 0

  override def setReviewedUpTo(project: Identifier, seq: Int): Unit = { }

  override def remove(project: Identifier): Unit = { }
}
