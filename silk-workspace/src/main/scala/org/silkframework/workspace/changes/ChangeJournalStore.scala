package org.silkframework.workspace.changes

import com.typesafe.config.Config
import org.silkframework.config.ConfigValue
import org.silkframework.runtime.plugin.annotations.PluginType
import org.silkframework.runtime.plugin.{AnyPlugin, PluginContext, PluginRegistry}
import org.silkframework.runtime.resource.{ResourceLoader, ResourceManager}
import org.silkframework.util.Identifier

import java.util.logging.Logger

/**
  * Holds the change journal entries of all projects. The configured store is a singleton; the [[ChangeJournal]]
  * of each project records into it. A store is passive: it never calls into the journal, the project or the workspace.
  */
@PluginType()
trait ChangeJournalStore extends AnyPlugin {

  /** Whether appended entries are kept; false for a store that discards them, e.g. the disabled journal. */
  def keepsEntries: Boolean = true

  /** What the journal locks on for a project: every read-then-write of the journal runs under it. A store that loads
    * a project's journal on first access answers one object per project, so only that project's writes wait for the
    * load. Default: one for all projects. */
  def monitor(project: Identifier): AnyRef = this

  /** Appends an entry to a project's journal. Entries arrive with increasing seq per project. The context is the
    * project's, for a store that serializes the entry. */
  def append(project: Identifier, entry: ChangeEntry)(implicit context: PluginContext): Unit

  /** The headers of all entries of a project, oldest first. Read on every write and by every listing, so a store keeps
    * them at hand. */
  def headers(project: Identifier): Seq[ChangeHeader]

  /** An entry of a project with its change, or None if the journal holds no entry with this seq. The context is the
    * project's, for a store that reads the change back. */
  def entry(project: Identifier, seq: Int)(implicit context: PluginContext): Option[ChangeEntry]

  /** The seq of a project's newest entry, 0 if it has none. Read on every write and needs no context. */
  def latestSeq(project: Identifier): Int

  /** The seq up to which no change of a project waits for review; 0 if never set. */
  def reviewedUpTo(project: Identifier): Int

  /** Sets the seq up to which no change of a project waits for review. */
  def setReviewedUpTo(project: Identifier, seq: Int): Unit

  /** Drops the journal of a project, including its reviewed watermark. Called when the project is deleted. */
  def remove(project: Identifier): Unit

  /** Writes a project's journal into the journal folder of a project export; nothing by default. */
  def exportJournal(project: Identifier, target: ResourceManager): Unit = { }

  /** Replaces a project's journal with the one in the journal folder of a project import. By default, a journal in
    * the archive is ignored with a log line. */
  def importJournal(project: Identifier, source: ResourceLoader): Unit = {
    if(source.listRecursive.nonEmpty) {
      ChangeJournalStore.log.info(s"The archive of project '$project' carries a change journal, which the '${pluginSpec.id}' store does not import.")
    }
  }
}

object ChangeJournalStore {

  private val log = Logger.getLogger(getClass.getName)

  private val instance: ConfigValue[ChangeJournalStore] = (config: Config) => {
    implicit val pluginContext: PluginContext = PluginContext.empty
    PluginRegistry.createFromConfigOption[ChangeJournalStore]("workspace.changes") match {
      case Some(store) =>
        log.info("Using configured change journal store " + store.pluginSpec.id)
        store
      case None =>
        log.info("No change journal store configured at configuration path 'workspace.changes.*'. No changes will be recorded.")
        EmptyChangeJournalStore()
    }
  }

  /** The configured change journal store; no changes are recorded, if none is configured. */
  def apply(): ChangeJournalStore = instance()
}
