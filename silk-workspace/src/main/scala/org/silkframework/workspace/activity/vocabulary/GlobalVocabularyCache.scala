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
  private var vocabsToUpdate: Set[String] = Set.empty
  @volatile
  private var lastUpdated: Option[Long] = None
  private def setLastUpdated(): Unit = {
    lastUpdated = Some(System.currentTimeMillis())
  }

  override def initialValue: Option[VocabularyCacheValue] = Some(new VocabularyCacheValue(Seq.empty, lastUpdated))

  override def run(context: ActivityContext[VocabularyCacheValue])(implicit userContext: UserContext): Unit = {
    val vocabManager = VocabularyManager()
    vocabsToUpdate = GlobalVocabularyCache.clearAndGetVocabularies
    while(vocabsToUpdate.nonEmpty && !cancelled) {
      // Update vocabs that were changed by a user
      for(vocabURI <- vocabsToUpdate) {
        installVocabulary(vocabManager, vocabURI)
        vocabsToUpdate -= vocabURI
      }
      context.value.update(new VocabularyCacheValue(cache.values.toSeq, lastUpdated))
      // Check if something has changed
      vocabsToUpdate ++= GlobalVocabularyCache.clearAndGetVocabularies
    }
    // Also update all vocabularies in case a former request has failed
    loadAllInstalledVocabularies(vocabManager)
    context.value.update(new VocabularyCacheValue(cache.values.toSeq, lastUpdated))
  }

  /* Loads all installed vocabularies and removes uninstalled ones.
     Vocabularies existing in the cache are never reloaded, i.e. new modifications to vocabularies are not detected.
   */
  private def loadAllInstalledVocabularies(vocabManager: VocabularyManager)
                                          (implicit userContext: UserContext): Unit = {
    vocabManager.retrieveGlobalVocabularies() match {
      case Some(vocabs) =>
        val installedVocabularies = vocabs.toSet
        // Install all vocabularies that are not loaded in the cache, yet
        for (vocabURI <- installedVocabularies if !cache.contains(vocabURI) && !cancelled) {
          installVocabulary(vocabManager, vocabURI)
        }
        // Remove uninstalled vocabs
        for (vocabURI <- cache.keys) {
          if (!installedVocabularies.contains(vocabURI)) {
            cache.remove(vocabURI)
            setLastUpdated()
            log.info(s"Vocabulary '$vocabURI' has been removed from the cache.")
          }
        }
      case None =>
        // Not possible to load or remove global vocabularies without this information
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

  def putVocabularyInQueue(vocabUri: String): Unit = synchronized {
    needsUpdate.add(vocabUri)
  }

  def clearAndGetVocabularies: Set[String] = synchronized {
    val result = needsUpdate.toSet
    needsUpdate.clear()
    result
  }
}
