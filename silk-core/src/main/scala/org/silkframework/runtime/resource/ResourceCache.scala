package org.silkframework.runtime.resource

import java.time.Instant
import java.util.logging.Logger

/**
  * Caches a value that has been read from a resource.
  *
  * Implementors need to implemented the load() method.
  *
  * @param resource The resource to be read.
  * @param updateTimeout Minimum time between cache updates in milliseconds.
  *                      The cache will only update if the modified time of the resource changed.
  */
abstract class ResourceCache[T](protected val resource: Resource, updateTimeout: Long = ResourceCache.DEFAULT_UPDATE_TIMEOUT) {

  // The logger of the implementation class
  private val log = Logger.getLogger(getClass.getName)

  // The cache value or None, if it has not been loaded yet
  private var cachedValue: Option[T] = None

  // The modification time of the resource when the cached value has been loaded
  private var lastModificationTime: Instant = Instant.EPOCH

  // The timestamp when the modification time of the resource has been checked the last time
  private var timestamp: Long = 0L

  /**
    * Retrieves the current value.
    * Will update the cache if needed before returning the value.
    */
  final def value: T = synchronized {
    // Not making this a val lets the class initialize without exception, which would else prevent the executing task from loading
    // and it allows to initialize at runtime, even if the resource was not readable at some moment in time.
    val needsUpdate = checkForUpdate()
    cachedValue match {
      case Some(v) if !needsUpdate =>
        v
      case _ =>
        updateCache()
    }
  }

  /**
   * Forces the cache to reload its value.
   */
  def updateNow(): Unit = synchronized {
    updateCache()
    lastModificationTime = Instant.now()
    timestamp = System.currentTimeMillis
  }

  /**
    * Loads the value from the resource.
    */
  protected def load(): T

  /**
    * Updates the cache and returns the updated value.
    */
  private def updateCache(): T = {
    log.info(s"Updating cached value from ${resource.name}.")
    val updatedValue = load()
    cachedValue = Some(updatedValue)
    updatedValue
  }

  /**
    * Checks if the cache needs to be updated, i.e., if the modified date has been updated after the updateTimeout passed.
    */
  private def checkForUpdate(): Boolean = {
    if(System.currentTimeMillis > timestamp + updateTimeout) {
      resource.modificationTime match {
        case Some(modificationTime) =>
          timestamp = System.currentTimeMillis
          if(modificationTime.isAfter(lastModificationTime)) {
            lastModificationTime = modificationTime
            true
          } else {
            false
          }
        case None =>
          true
      }
    } else {
      false
    }
  }

}

object ResourceCache {

  val DEFAULT_UPDATE_TIMEOUT: Long = 5000L

}
