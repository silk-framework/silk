package de.fuberlin.wiwiss.silk.config

/**
 * The runtime configuration.
 *
 * @param blocking The blocking parameters.
 * @param useFileCache If true, the entities are cached on the file system. If false, an in-memory cache is used.
 * @param reloadCache Specifies if the entity cache is to be reloaded before executing the matching.
 * @param partitionSize The maximum size of the entity partitions in the cache.
 * @param numThreads The number of concurrent threads used for matching.
 * @param generateDetailedLinks Generate links with detailed information.
 * @param homeDir The directory used by Silk to store persistent information such as caches.
 */
case class RuntimeConfig(blocking: Blocking = Blocking(),
                         useFileCache: Boolean = true,
                         reloadCache: Boolean = true,
                         partitionSize: Int = 1000,
                         numThreads: Int = 8,
                         generateDetailedLinks: Boolean = false,
                         homeDir: String = System.getProperty("user.home") + "/.silk/") {
  require(partitionSize > 1, "partitionSize must be greater than 0 (partitionSize=" + partitionSize + ")")
}