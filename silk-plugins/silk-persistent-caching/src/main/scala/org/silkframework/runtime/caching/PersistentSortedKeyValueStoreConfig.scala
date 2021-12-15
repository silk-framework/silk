package org.silkframework.runtime.caching

/**
  * Config properties for the persistent, sorted key value store.
  *
  * @param maxSizeInBytes The maximum size of the DB. Overestimating is no problem.
  * @param truncateKeys   If true, keys larger than the value returned from maxKeySize() are truncated to that value.
  *                       If false, an IllegalArgumentException is thrown.
  * @param compressKeys   If compression should be used for keys
  * @param compressValues If compression should be used for values.
  */
case class PersistentSortedKeyValueStoreConfig(maxSizeInBytes: Long = PersistentSortedKeyValueStore.defaultMaxSizeInBytes(),
                                               truncateKeys: Boolean = true,
                                               compressKeys: Boolean = false,
                                               compressValues: Boolean = false)
