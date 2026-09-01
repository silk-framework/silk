package org.silkframework.workspace.changes

import com.typesafe.config.Config
import org.silkframework.config.ConfigValue
import org.silkframework.runtime.plugin.annotations.PluginType
import org.silkframework.runtime.plugin.{AnyPlugin, PluginContext, PluginRegistry}
import org.silkframework.util.Identifier

import java.util.logging.Logger

/**
  * Holds the change journal entries of all projects. The configured store is a singleton; the [[ChangeJournal]]
  * of each project records into it.
  */
@PluginType()
trait ChangeJournalStore extends AnyPlugin {

  /** Appends an entry to a project's journal. Entries arrive with increasing seq per project. */
  def append(project: Identifier, entry: ChangeEntry): Unit

  /** All entries of a project, oldest first. */
  def entries(project: Identifier): Seq[ChangeEntry]

  /** The seq up to which the user has reviewed a project's changes; 0 if never set. */
  def reviewedUpTo(project: Identifier): Int

  /** Sets the seq up to which the user has reviewed a project's changes. */
  def setReviewedUpTo(project: Identifier, seq: Int): Unit

  /** Drops the journal of a project, including its reviewed watermark. Called when the project is deleted. */
  def remove(project: Identifier): Unit
}

object ChangeJournalStore {

  private val log = Logger.getLogger(getClass.getName)

  private val instance: ConfigValue[ChangeJournalStore] = (config: Config) => {
    if(config.hasPath("workspace.changes")) {
      implicit val pluginContext: PluginContext = PluginContext.empty
      val store = PluginRegistry.createFromConfig[ChangeJournalStore]("workspace.changes")
      log.info("Using configured change journal store " + config.getString("workspace.changes.plugin"))
      store
    } else {
      InMemoryChangeJournalStore()
    }
  }

  /** The configured change journal store; in-memory, if none is configured. */
  def apply(): ChangeJournalStore = instance()
}
