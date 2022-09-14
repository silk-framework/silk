package org.silkframework.runtime.caching

import org.apache.commons.codec.digest.DigestUtils

/**
  * Config properties for the persistent, sorted key value store.
  *
  * @param maxSizeInBytes The maximum size of the DB. Overestimating is no problem.
  * @param tooLargeKeyStrategy For keys larger than the value returned from maxKeySize() are truncated to that value.
  *                       Order of keys can change, uniqueness of keys is very unlikely to be affected.
  *                       If false, an IllegalArgumentException is thrown.
  * @param compressKeys   If compression should be used for keys
  * @param compressValues If compression should be used for values.
  */
case class PersistentSortedKeyValueStoreConfig(tooLargeKeyStrategy: HandleTooLargeKeyStrategy,
                                               maxSizeInBytes: Long = PersistentSortedKeyValueStore.defaultMaxSizeInBytes(),
                                               compressKeys: Boolean = false,
                                               compressValues: Boolean = false)

sealed trait HandleTooLargeKeyStrategy

object HandleTooLargeKeyStrategy {
  /** Throw an error when a key is found that is larger than the allowed key size. */
  object ThrowError extends HandleTooLargeKeyStrategy

  /** Truncate the key from the right, e.g. ABC...XYZ becomes ABC...X.
    * This will keep the order of the keys and may lead to clashes if 2 different strings have the same truncated string. */
  object TruncateKey extends HandleTooLargeKeyStrategy

  /** Same as TruncateKey, but calculates a SHA1 hash that is appended to the key.
    * This will keep the order of the keys based on their truncated prefix and keep keys (very, very likely) unique.
    */
  object TruncateKeyWithHash extends HandleTooLargeKeyStrategy

  /** Returns either the key if it fits or returns the truncated key with a SHA-256 digest value appended to it. */
  def truncateKeyWithHash(key: Array[Byte], maxKeyLength: Int): Array[Byte] = {
    if(key.length > maxKeyLength) {
      val newKeyBytes = new Array[Byte](maxKeyLength)
      val keyDigest = DigestUtils.sha256(key)
      assert(keyDigest.length < maxKeyLength, "Max key length is too short to store the hash in it.")
      val keyPrefixLength = maxKeyLength - keyDigest.length
      Array.copy(key, 0, newKeyBytes, 0, keyPrefixLength)
      Array.copy(keyDigest, 0, newKeyBytes, keyPrefixLength, keyDigest.length)
      newKeyBytes
    } else {
      key
    }
  }
}