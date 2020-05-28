package org.silkframework.runtime.resource

import java.time.Instant

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

  private var _value: Option[T] = None

  private var timestamp: Long = 0L

  private var lastModificationTime: Instant = Instant.EPOCH

  /**
    * Retrieves the current value.
    * Will update the cache if needed before returning the value.
    */
  final def value: T = synchronized {
    // Not making this a val lets the class initialize without exception, which would else prevent the executing task from loading
    // and it allows to initialize at runtime, even if the resource was not readable at some moment in time.
    _value match {
      case Some(vm) if !needsUpdate() =>
        vm
      case _ =>
        updateCache()
    }
  }

  /**
    * Loads the value from the resource.
    */
  protected def load(): T

  /**
    * Updates the cache and returns the updated value.
    */
  private def updateCache(): T = {
    val updatedValue = load()
    timestamp = System.currentTimeMillis
    _value = Some(updatedValue)
    updatedValue
  }

  /**
    * Checks if the cache needs to be updated, i.e., if the modified date has been updated after the updateTimeout passed.
    */
  private def needsUpdate(): Boolean = {
    if(System.currentTimeMillis > timestamp + updateTimeout) {
      resource.modificationTime match {
        case Some(modificationTime) =>
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
