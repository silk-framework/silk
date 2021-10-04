package org.silkframework.workspace.activity.transform

import org.silkframework.config.DefaultConfig
import org.silkframework.rule._
import org.silkframework.runtime.activity.{Activity, ActivityContext, UserContext}
import org.silkframework.runtime.plugin.annotations.Plugin
import org.silkframework.util.Identifier
import org.silkframework.workspace.WorkspaceFactory
import org.silkframework.workspace.activity.GlobalWorkspaceActivityFactory

import java.time.{Duration, Instant}
import java.util.logging.Logger
import scala.collection.mutable

@Plugin(
  id = "GlobalUriPatternCache",
  label = "Global URI pattern cache",
  categories = Array("TransformSpecification"),
  description = "Caches URI patterns extracted from existing mappings."
)
case class GlobalUriPatternCacheFactory() extends GlobalWorkspaceActivityFactory[GlobalUriPatternCache] {
  override def autoRun: Boolean = true

  override def apply(): Activity[GlobalUriPatternCacheValue] = {
    GlobalUriPatternCache()
  }
}

/** Caches synonyms for vocabulary entities. */
case class GlobalUriPatternCache() extends Activity[GlobalUriPatternCacheValue] {

  private val log: Logger = Logger.getLogger(getClass.getName)

  @volatile
  private var lastRun: Option[Instant] = None

  @volatile
  private var loadedProjects: Set[Identifier] = Set.empty

  @volatile
  // In order to log "not enabled" message only once during start
  private var disabledMessageLogged = false

  // From target class URI to URI patterns
  private val uriPatternMap: mutable.HashMap[String, mutable.HashSet[String]] = mutable.HashMap()

  private def immutableValue(): Map[String, Set[String]] = uriPatternMap.toMap map { case (key, values) => (key, values.toArray.toSet)}

  // Full runs every 10 minutes, else only synonyms are added. This should reduce the load when the cache is updated a lot.
  final val DURATION_BETWEEN_FULL_RUNS = Duration.ofMinutes(10)

  private def modifiedSinceLastRun(modified: Option[Instant]): Boolean = modified match {
    case Some(lastModified) => lastRun.forall(lastModified.isAfter)
    case None => false
  }

  private def synonymCacheDisabled: Boolean = {
    val cfg = DefaultConfig.instance()
    !cfg.hasPath(GlobalUriPatternCache.CONFIG_KEY_ENABLED) || !cfg.getBoolean(GlobalUriPatternCache.CONFIG_KEY_ENABLED)
  }

  /**
    * Executes this activity.
    *
    * @param context Holds the context in which the activity is executed.
    */
  override def run(context: ActivityContext[GlobalUriPatternCacheValue])(implicit userContext: UserContext): Unit = {
    if(synonymCacheDisabled) {
      logDisabledMessage()
      return
    }
    val fullRun = durationBetweenFullRunsElapsed()
    if(fullRun) {
      uriPatternMap.clear()
    }
    val workspace = WorkspaceFactory().workspace
    val projects = workspace.projects
    for(project <- projects;
        transformTask <- project.tasks[TransformSpec] if fullRun || !loadedProjects.contains(project.name) || modifiedSinceLastRun(transformTask.metaData.modified)) {
      GlobalUriPatternCache.extractUriPatterns(transformTask.data.mappingRule, uriPatternMap)
    }
    lastRun = Some(Instant.now())
    loadedProjects = projects.map(_.name).toSet
    if(fullRun) {
      val uriPatterns = if(uriPatternMap.nonEmpty) uriPatternMap.map(_._2.size).sum else 0
      log.info(s"Extracted overall $uriPatterns URI patterns for ${uriPatternMap.size} target classes.")
    }
    context.value.update(GlobalUriPatternCacheValue(immutableValue(), Instant.now(), loadedProjects))
  }

  private def durationBetweenFullRunsElapsed(): Boolean = {
    lastRun.forall { lastRunTimestamp =>
      Instant.now.isAfter(lastRunTimestamp.plus(DURATION_BETWEEN_FULL_RUNS))
    }
  }

  private def logDisabledMessage(): Unit = {
    if (!disabledMessageLogged) {
      log.info(s"Global URI pattern cache is disabled, skipping URI pattern extraction in global URI pattern cache. Parameter: ${GlobalUriPatternCache.CONFIG_KEY_ENABLED}")
    }
    disabledMessageLogged = true
  }
}

/** The cached URI patterns.
  *
  * @param uriPatterns    A Map from target class URI to URI pattern.
  * @param lastUpdated    The time when this value has last been updated.
  * @param loadedProjects The projects that URI patterns were extracted from.
  */
case class GlobalUriPatternCacheValue(uriPatterns: Map[String, Set[String]],
                                      lastUpdated: Instant,
                                      loadedProjects: Set[Identifier]) {
  def uriPatternsForTargetClass(classUri: String): Set[String] = {
    uriPatterns.getOrElse(classUri, Set.empty)
  }
}

object GlobalUriPatternCache {
  final val CONFIG_KEY_ENABLED = "caches.global.uriPatternCache.enabled"
  final val CONFIG_KEY_TIME_BETWEEN_REFRESHES = "caches.global.uriPatternCache.timeBetweenRefreshes"
  final val CONFIG_KEY_WAIT_FOR_CACHE_TO_FINISH = "caches.global.uriPatternCache.waitForCacheToFinish"

  /** Extracts all URI patterns from a transform rule and connects them with the target type URIs used in the mappings. */
  def extractUriPatterns(transformRule: TransformRule,
                         uriPatternMap: mutable.HashMap[String, mutable.HashSet[String]]): Unit = {
    // Only use the last path operator as synonym
    val mappingRules = transformRule match {
      case objectMapping: ObjectMapping =>
        objectMapping.rules
      case rootMapping: RootMappingRule =>
        rootMapping.rules
      case _ => MappingRules()
    }
    for(typeRule <- mappingRules.typeRules;
        PatternUriMapping(_, pattern, _, _) <- mappingRules.uriRule) {
      val uriPatterns = uriPatternMap.getOrElseUpdate(typeRule.typeUri.uri, new mutable.HashSet[String]())
      uriPatterns.add(pattern)
    }

    transformRule.rules.propertyRules.foreach { rule =>
      extractUriPatterns(rule, uriPatternMap)
    }
  }
}