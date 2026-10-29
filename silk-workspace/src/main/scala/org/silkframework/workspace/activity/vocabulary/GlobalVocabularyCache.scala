package org.silkframework.workspace.activity.vocabulary

import org.silkframework.rule.vocab.{Vocabulary, VocabularyManager}
import org.silkframework.runtime.activity.{Activity, ActivityContext, UserContext}
import org.silkframework.runtime.plugin.annotations.Plugin
import org.silkframework.workspace.activity.GlobalWorkspaceActivityFactory
import org.silkframework.workspace.activity.transform.VocabularyCacheValue

import java.util.logging.Logger
import scala.collection.mutable

@Plugin(
  id = "GlobalVocabularyCache",
  label = "Global vocabulary cache",
  categories = Array("Vocabularies"),
  description = "Caches global vocabularies that can be accessed from everywhere."
)
case class GlobalVocabularyCacheFactory() extends GlobalWorkspaceActivityFactory[GlobalVocabularyCache] {
  override def autoRun: Boolean = true

  override def apply(): Activity[VocabularyCacheValue] = {
    GlobalVocabularyCache()
  }

  override def isCacheActivity: Boolean = true
}

/** A cache for global vocabularies that can be used in any project. */
case class GlobalVocabularyCache() extends Activity[VocabularyCacheValue] {
  private val log: Logger = Logger.getLogger(getClass.getName)
  private val cache: mutable.HashMap[String, Vocabulary] = new mutable.HashMap[String, Vocabulary]()
  @volatile
  private var lastUpdated: Option[Long] = None
  private def setLastUpdated(): Unit = {
    lastUpdated = Some(System.currentTimeMillis())
  }

  override def reset()(implicit userContext: UserContext): Unit = {
    // Mark all vocabularies for reload, but do not clean the cache, so it becomes eventually updated, but still delivers stale results for a while.
    super.reset()
    cache.keysIterator.foreach(key => GlobalVocabularyCache.putVocabularyInQueue(key))
  }

  override def initialValue: Option[VocabularyCacheValue] = Some(new VocabularyCacheValue(Seq.empty, lastUpdated))

  override def run(context: ActivityContext[VocabularyCacheValue])(implicit userContext: UserContext): Unit = {
    val vocabManager = VocabularyManager()
    // Always reconcile the cache at least once. Keep going while new force-update requests arrive during a run.
    val pendingUpdates = mutable.Set[String]()
    pendingUpdates ++= GlobalVocabularyCache.clearAndGetVocabularies
    try {
      do {
        reconcileCache(vocabManager, pendingUpdates)
        context.value.update(new VocabularyCacheValue(cache.values.toSeq, lastUpdated))
        pendingUpdates ++= GlobalVocabularyCache.clearAndGetVocabularies
      } while (pendingUpdates.nonEmpty && !cancelled)
    } finally {
      // Requests this run could not process (cancellation or failure) are re-queued instead of being lost
      pendingUpdates.foreach(GlobalVocabularyCache.putVocabularyInQueue)
    }
  }

  /* Reconciles the cache with the list of currently installed vocabularies:
       - installs vocabularies that are installed but not cached yet,
       - re-fetches installed vocabularies that were explicitly requested to be force-reloaded (`forcedUpdates`),
       - removes vocabularies that are no longer installed.
     Force-update requests are always checked against the installed vocabulary list first, so requests for vocabularies
     that are not (or no longer) installed are ignored instead of creating empty cache entries.
     If the vocabulary manager cannot provide the list of installed vocabularies, the requested vocabularies are
     force-loaded without this check (the list cannot be reconciled in that case).
     Processed (or ignored) requests are removed from `forcedUpdates`; requests still in the set afterwards were not
     handled, e.g., because the run was cancelled or an installation failed, and are re-queued by run().
   */
  private def reconcileCache(vocabManager: VocabularyManager,
                             forcedUpdates: mutable.Set[String])
                            (implicit userContext: UserContext): Unit = {
    vocabManager.retrieveGlobalVocabularies() match {
      case Some(vocabs) =>
        val installedVocabularies = vocabs.toSet
        if(forcedUpdates.nonEmpty) {
          log.info(s"Going to update ${forcedUpdates.count(installedVocabularies.contains)} of ${forcedUpdates.size} requested vocabularies...")
        }
        // Requests for vocabularies that are not installed are ignored (dropped, not re-queued)
        forcedUpdates.filterInPlace(installedVocabularies.contains)
        // Install vocabularies that are not cached yet or that were explicitly requested to be force-reloaded.
        for (vocabURI <- installedVocabularies if !cancelled && (!cache.contains(vocabURI) || forcedUpdates.contains(vocabURI))) {
          installVocabulary(vocabManager, vocabURI)
          forcedUpdates.remove(vocabURI)
        }
        // Remove uninstalled vocabularies.
        for (vocabURI <- cache.keys.toSeq if !installedVocabularies.contains(vocabURI)) {
          cache.remove(vocabURI)
          setLastUpdated()
          log.info(s"Vocabulary '$vocabURI' has been removed from the cache.")
        }
      case None =>
        // The list of installed vocabularies is not available, so force-update the requested vocabularies without it.
        for (vocabURI <- forcedUpdates.toSeq if !cancelled) {
          installVocabulary(vocabManager, vocabURI)
          forcedUpdates.remove(vocabURI)
        }
    }
  }

  private def installVocabulary(vocabManager: VocabularyManager,
                                vocabURI: String)
                               (implicit userContext: UserContext): Unit = {
    val startTime = System.currentTimeMillis()
    var updated = false
    vocabManager.get(vocabURI, None) foreach { vocabulary =>
      cache.put(vocabURI, vocabulary)
      setLastUpdated()
      updated = true
    }
    if(updated) {
      log.info(s"Vocabulary '$vocabURI' has been updated in ${System.currentTimeMillis() - startTime}ms.")
    } else {
      log.warning(s"Processed request to update vocabulary '$vocabURI', but no vocabulary has been found.")
    }
  }
}

object GlobalVocabularyCache {
  private val needsUpdate = mutable.HashSet[String]()

  /** Queues a single vocabulary to be force-reloaded on the next cache run. This is the only way to refresh the content of a
    * vocabulary that is already in the cache, since the general reconciliation otherwise only adds newly installed
    * vocabularies and removes uninstalled ones without re-fetching cached entries. The queued vocabulary is only
    * re-fetched if it is part of the installed vocabulary list when the cache runs. */
  def putVocabularyInQueue(vocabUri: String): Unit = synchronized {
    needsUpdate.add(vocabUri)
  }

  def clearAndGetVocabularies: Set[String] = synchronized {
    val result = needsUpdate.toSet
    needsUpdate.clear()
    result
  }
}
