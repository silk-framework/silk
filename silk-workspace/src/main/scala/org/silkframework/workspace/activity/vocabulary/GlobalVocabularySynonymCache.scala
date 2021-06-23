package org.silkframework.workspace.activity.vocabulary

import org.silkframework.config.DefaultConfig
import org.silkframework.entity.paths.DirectionalPathOperator
import org.silkframework.rule.{MappingRules, TransformRule, TransformSpec}
import org.silkframework.runtime.activity.{Activity, ActivityContext, UserContext}
import org.silkframework.runtime.plugin.annotations.Plugin
import org.silkframework.util.Uri
import org.silkframework.workspace.WorkspaceFactory
import org.silkframework.workspace.activity.GlobalWorkspaceActivityFactory
import org.silkframework.workspace.activity.transform.VocabularyCacheValue

import java.time.Instant
import java.util.logging.Logger
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

@Plugin(
  id = "GlobalVocabularySynonymCache",
  label = "Global vocabulary synonym cache",
  categories = Array("Vocabularies"),
  description = "Caches synonyms for vocabulary elements extracted from different sources, e.g. existing mappings."
)
case class GlobalVocabularySynonymCacheFactory() extends GlobalWorkspaceActivityFactory[GlobalVocabularySynonymCache] {
  override def autoRun: Boolean = true

  override def apply(): Activity[GlobalVocabularySynonymCacheValue] = {
    GlobalVocabularySynonymCache()
  }
}

case class GlobalVocabularySynonymCache() extends Activity[GlobalVocabularySynonymCacheValue] {
  private val log: Logger = Logger.getLogger(getClass.getName)
  @volatile
  private var lastRun: Option[Long] = None

  @volatile
  private var disabledMessageLogged = false

  private val synonymMap: mutable.HashMap[String, ArrayBuffer[String]] = mutable.HashMap()

  // Full runs every 10 minutes, else only synonyms are added. This should reduce the load when the cache is updated a lot.
  final val DURATION_BETWEEN_FULL_RUNS = 10 * 60 * 1000

  private def modifiedSinceLastRun(modified: Option[Instant]): Boolean = modified match {
    case Some(instant) => instant.toEpochMilli > lastRun.getOrElse(0L)
    case None => false
  }

  override def run(context: ActivityContext[GlobalVocabularySynonymCacheValue])
                  (implicit userContext: UserContext): Unit = {
    if(synonymCacheDisabled) {
      logDisabledMessage()
      return
    }
    val workspace = WorkspaceFactory().workspace
    val globalVocabularyCacheActivity = workspace.activity[GlobalVocabularyCache]
    val vocabularyCacheValueOpt = workspace.activity[GlobalVocabularyCache].value.get
    val vocabularyCacheValue = vocabularyCacheValueOpt match {
      case Some(cache) => cache
      case None =>
        // Might be currently running, wait for it and fetch the value again.
        globalVocabularyCacheActivity.control.waitUntilFinished()
        if(!globalVocabularyCacheActivity.value.isDefined) {
          log.warning(s"Global vocabulary cache has not been correctly initialized. Not able to extract synonyms for vocabulary properties!")
          return
        }
        globalVocabularyCacheActivity.value()
    }
    val fullRun = lastRun.isEmpty || (System.currentTimeMillis() - lastRun.get > DURATION_BETWEEN_FULL_RUNS)
    if(fullRun) {
      synonymMap.clear()
    }
    val projects = workspace.projects
    for(project <- projects;
        transformTask <- project.tasks[TransformSpec] if fullRun || modifiedSinceLastRun(transformTask.metaData.modified)) {
      GlobalVocabularySynonymCache.extractPropertySynonyms(transformTask.data.mappingRule, synonymMap, vocabularyCacheValue)
    }
    lastRun = Some(System.currentTimeMillis())
    if(fullRun) {
      val nrSynonyms = if(synonymMap.nonEmpty) synonymMap.map(_._2.size).sum else 0
      log.info(s"Extracted overall $nrSynonyms for ${synonymMap.size} properties.")
    }
    context.value.update(new GlobalVocabularySynonymCacheValue(immutableValue()))
  }

  private def synonymCacheDisabled: Boolean = {
    val cfg = DefaultConfig.instance()
    !cfg.hasPath(GlobalVocabularySynonymCache.CONFIG_KEY_ENABLED) || !cfg.getBoolean(GlobalVocabularySynonymCache.CONFIG_KEY_ENABLED)
  }

  private def logDisabledMessage(): Unit = {
    if (!disabledMessageLogged) {
      log.info(s"Mapping suggestion via vocabulary synonyms is disabled, skipping synonym extraction in global vocabulary cache. Parameter: ${GlobalVocabularySynonymCache.CONFIG_KEY_ENABLED}")
    }
    disabledMessageLogged = true
  }

  private def immutableValue(): Map[String, Seq[String]] = synonymMap.toMap map { case (key, values) => (key, values.toArray.toSeq)}
}

/** Synonyms for properties. */
class GlobalVocabularySynonymCacheValue(propertySynonyms: Map[String, Seq[String]]) {
  def synonymsForProperty(propertyUri: String): Seq[String] = {
    propertySynonyms.getOrElse(propertyUri, Seq.empty)
  }
}

object GlobalVocabularySynonymCache {
  final val CONFIG_KEY_ENABLED = "mapping.suggestion.features.extractSynonymsFromExistingMappingRules.enabled"

  /** Extracts property synonyms */
  def extractPropertySynonyms(transformRule: TransformRule,
                              synonymMap: mutable.HashMap[String, ArrayBuffer[String]],
                              vocabularyCacheValue: VocabularyCacheValue): Unit = {
    if(isSynonymExtractionCandidate(transformRule, vocabularyCacheValue)) {
      val targetUri = transformRule.target.get.propertyUri.uri
      // Only use the last path operator as synonym
      val significantPathOperator = transformRule.sourcePaths.head.operators.last
      val property = significantPathOperator.asInstanceOf[DirectionalPathOperator].property
      val sourceLabel = if(property.isValidUri) {
        // Extract label from real URI
        Uri.urlDecodedLocalNameOfURI(property)
      } else {
        // This is no real URI, assume a normal element/attribute name, e.g. from XML, CSV etc.
        property.uri
      }
      val synonyms = synonymMap.getOrElseUpdate(targetUri, new ArrayBuffer[String]())
      synonyms.append(sourceLabel)
    }
    transformRule.rules.propertyRules.foreach { rule =>
      extractPropertySynonyms(rule, synonymMap, vocabularyCacheValue)
    }
  }

  /** Returns true if this rule can be used to extract synonyms */
  private def isSynonymExtractionCandidate(transformRule: TransformRule,
                                           vocabularyCacheValue: VocabularyCacheValue) = {
    // Only extract synonyms from 1-to-1 mapping rules
    transformRule.target.isDefined &&
      // Only extract synonyms for real target URIs and not from mappings to other formats, e.g. CSV.
      transformRule.target.get.propertyUri.isValidUri &&
      // This is a property from one of the target vocabularies used matching in the mapping suggestion
      vocabularyCacheValue.findProperty(transformRule.target.get.propertyUri).isDefined &&
      // Ignore rules that are composed of multiple source paths were it's unclear which source path maps to the target property
      transformRule.sourcePaths.size == 1 && transformRule.sourcePaths.head.operators.nonEmpty &&
      // Only match simple paths without additional logic that adds unknown semantics to the source path
      transformRule.sourcePaths.head.operators.forall(_.isInstanceOf[DirectionalPathOperator])
  }
}