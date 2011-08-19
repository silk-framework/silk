package de.fuberlin.wiwiss.silk.workbench.lift.util

import io.Source
import java.util.logging.{Level, Logger}
import de.fuberlin.wiwiss.silk.config.Prefixes

/**
 * Registry of known prefixes.
 */
object PrefixRegistry {
  private val logger = Logger.getLogger(getClass.getName)

  /**
   * Map of all known prefixes.
   */
  lazy val all: Prefixes = {
    try {
      val prefixStream = getClass.getClassLoader.getResourceAsStream("de/fuberlin/wiwiss/silk/workbench/lift/prefixes.csv")
      val prefixSource = Source.fromInputStream(prefixStream)
      val prefixLines = prefixSource.getLines

      val prefixMap = prefixLines.map(_.split(',')).map {
        case Array(id, namespace) => (id, namespace.drop(1).dropRight(1))
      }.toMap

      val validPrefixes = prefixMap.filter {
        case (id, namespace) => !namespace.isEmpty
      }

      Prefixes(validPrefixes)
    } catch {
      case ex: Exception => {
        logger.log(Level.WARNING, "Error loading prefix table. ", ex)
        Prefixes.empty
      }
    }
  }
}