package cacheUpdater

import akka.actor.ActorSystem
import org.silkframework.config.{DefaultConfig, ExtendedTypesafeConfig}
import org.silkframework.dataset.DirtyTrackingFileDataSink
import org.silkframework.runtime.activity.UserContext
import org.silkframework.workspace.WorkspaceFactory
import play.api.libs.concurrent.CustomExecutionContext
import resources.ResourceHelper

import java.time.temporal.ChronoUnit
import java.time.{Duration, Instant}
import java.util.concurrent.TimeUnit
import java.util.logging.Logger
import javax.inject.Inject
import scala.concurrent.duration._
import scala.util.Try

/** Checks for caches that need to be updated. */
class CacheUpdaterTask @Inject() (actorSystem: ActorSystem, executionContext: CacheUpdaterTaskExecutionContext) {
  private val log: Logger = Logger.getLogger(this.getClass.getName)
  @volatile
  private var lastUpdate: Long = 0L

  CacheUpdaterTask.interval() match {
    case Some(interval) =>
      log.fine(s"Starting cache updater with interval '${interval.toString()}'. Interval can be configured via config parameter '${CacheUpdaterTask.INTERVAL_CONFIG_KEY}'." +
        s" An invalid duration value will disable the cache updater.")
      actorSystem.scheduler.scheduleAtFixedRate(initialDelay = interval, interval = interval) { () =>
        updateTypeAndPathCaches(interval)
      }(executionContext)
    case None =>
      log.info(s"Cache updater module is disabled. It can be enabled by setting a valid duration value for config parameter '${CacheUpdaterTask.INTERVAL_CONFIG_KEY}'.")
  }

  // Update type and paths caches based on updated project files
  private def updateTypeAndPathCaches(interval: FiniteDuration): Unit = {
    val updateStart = System.currentTimeMillis()
    val updatedFiles = DirtyTrackingFileDataSink.fetchAndClearUpdatedFiles()
    val minExtraToleranceInMs = 1000L
    val lastUpdateInstant = Instant.ofEpochMilli(lastUpdate).minus(math.max(interval.toMillis, minExtraToleranceInMs), ChronoUnit.MILLIS)
    if(updatedFiles.nonEmpty) {
      log.info(s"Cache updater task running. ${updatedFiles.size} updated file(s) found. Triggering cache updates...")
      implicit val userContext: UserContext = UserContext.INTERNAL_USER
      val workspace = WorkspaceFactory().workspace
      val projects = workspace.projects
      for(project <- projects) {
        val pr = project.resources
        val updatedProjectFiles = pr.listRecursive.toSet.intersect(updatedFiles)
          .map(fileName => pr.get(fileName))
        // Check that the file was actually changed since last run
        val reallyChangedFiles = updatedProjectFiles
          .filter(f => f.modificationTime.exists(_.isAfter(lastUpdateInstant)))
        reallyChangedFiles.foreach(f => ResourceHelper.refreshCachesOfDependingTasks(f.name, project))
      }
    }
    lastUpdate = updateStart
  }
}

object CacheUpdaterTask {
  final val INTERVAL_CONFIG_KEY = "cacheUpdater.updateInterval"

  /** Returns the interval after which the cache updater should be re-run. */
  def interval(): Option[FiniteDuration] = {
    val cfg: ExtendedTypesafeConfig = DefaultConfig.instance.extendedTypesafeConfig()
    val DEFAULT_INTERVAL = 60
    Try(cfg.getDurationOrElse(INTERVAL_CONFIG_KEY, Duration.ofSeconds(DEFAULT_INTERVAL))).toOption
      .map(d => FiniteDuration(d.toNanos, TimeUnit.NANOSECONDS))
  }
}

class CacheUpdaterTaskExecutionContext @Inject() (actorSystem: ActorSystem)
  extends CustomExecutionContext(actorSystem, "cache-updater")