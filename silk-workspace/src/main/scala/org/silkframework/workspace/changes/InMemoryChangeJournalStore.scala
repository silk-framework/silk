package org.silkframework.workspace.changes

import org.silkframework.runtime.plugin.annotations.{Param, Plugin}
import org.silkframework.util.Identifier

@Plugin(
  id = "inMemoryChangeJournal",
  label = "Changes held in memory",
  description = "Holds the most recent changes of each project in memory."
)
case class InMemoryChangeJournalStore(@Param("The number of entries kept per project.")
                                      maxEntries: Int = 500) extends ChangeJournalStore {

  private var journals = Map.empty[Identifier, Vector[ChangeEntry]]

  override def append(project: Identifier, entry: ChangeEntry): Unit = synchronized {
    journals += project -> (journals.getOrElse(project, Vector.empty) :+ entry).takeRight(maxEntries)
  }

  override def entries(project: Identifier): Seq[ChangeEntry] = synchronized {
    journals.getOrElse(project, Vector.empty)
  }

  override def remove(project: Identifier): Unit = synchronized {
    journals -= project
  }
}
