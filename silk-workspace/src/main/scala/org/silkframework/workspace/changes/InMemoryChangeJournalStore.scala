package org.silkframework.workspace.changes

import org.silkframework.runtime.plugin.PluginContext
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

  private var reviewed = Map.empty[Identifier, Int]

  override def append(project: Identifier, entry: ChangeEntry)(implicit context: PluginContext): Unit = synchronized {
    journals += project -> (journals.getOrElse(project, Vector.empty) :+ entry).takeRight(maxEntries)
  }

  override def entries(project: Identifier)(implicit context: PluginContext): Seq[ChangeEntry] = synchronized {
    journals.getOrElse(project, Vector.empty)
  }

  override def latestSeq(project: Identifier): Int = synchronized {
    journals.get(project).flatMap(_.lastOption).map(_.seq).getOrElse(0)
  }

  override def reviewedUpTo(project: Identifier): Int = synchronized {
    reviewed.getOrElse(project, 0)
  }

  override def setReviewedUpTo(project: Identifier, seq: Int): Unit = synchronized {
    reviewed += project -> seq
  }

  override def remove(project: Identifier): Unit = synchronized {
    journals -= project
    reviewed -= project
  }
}
