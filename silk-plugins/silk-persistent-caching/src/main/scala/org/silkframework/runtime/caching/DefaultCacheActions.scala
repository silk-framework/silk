package org.silkframework.runtime.caching

import java.nio.ByteBuffer

import org.silkframework.dataset.rdf.ClosableIterator

/**
  * Implements default actions that make sense for most concrete cache implementations using the [[PersistentSortedKeyValueStore]].
  */
trait DefaultCacheActions {
  /** The store on which the default actions should be executed on. */
  protected def store: PersistentSortedKeyValueStore

  /** Re-initializes the key value stores. Removes all entries from all caches. */
  def cleanStore(): Unit = {
    store.clearStore()
  }

  /** Number of entries in the store. */
  def storeSize(): Long = store.size()

  /** Iterate over all entries as ByteBuffer pairs. */
  def iterateEntries(): ClosableIterator[(ByteBuffer, ByteBuffer)] = {
    store.iterateEntries()
  }

  /** Iterate over all entries as String key, String value pairs. */
  def stringIterator(): ClosableIterator[(String, String)] = {
    store.iterateStringEntries()
  }
}
