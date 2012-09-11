/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.fuberlin.wiwiss.silk.config

/**
 * The runtime configuration.
 *
 * @param blocking The blocking parameters.
 * @param useFileCache If true, the entities are cached on the file system. If false, an in-memory cache is used.
 * @param reloadCache Specifies if the entity cache is to be reloaded before executing the matching.
 * @param partitionSize The maximum size of the entity partitions in the cache.
 * @param numThreads The number of concurrent threads used for matching.
 * @param generateLinksWithEntities Generate links with the entities they connect.
 * @param homeDir The directory used by Silk to store persistent information such as caches.
 */
case class RuntimeConfig(blocking: Blocking = Blocking(),
                         useFileCache: Boolean = true,
                         reloadCache: Boolean = true,
                         partitionSize: Int = 10000,
                         numThreads: Int = Runtime.getRuntime.availableProcessors(),
                         generateLinksWithEntities: Boolean = false,
                         homeDir: String = System.getProperty("user.home") + "/.silk/") {
  require(partitionSize > 1, "partitionSize must be greater than 0 (partitionSize=" + partitionSize + ")")
}