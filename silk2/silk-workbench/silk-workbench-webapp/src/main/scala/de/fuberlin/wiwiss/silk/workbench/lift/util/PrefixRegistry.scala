package de.fuberlin.wiwiss.silk.workbench.lift.util

import io.Source
import java.util.logging.{Level, Logger}

/**
 * Registry of known prefixes.
 */
object PrefixRegistry
{
  private val logger = Logger.getLogger(getClass.getName)

  /**
   * Map of all known prefixes.
   */
  lazy val all : Map[String, String] =
  {
    try
    {
      val prefixStream = getClass.getClassLoader.getResourceAsStream("de/fuberlin/wiwiss/silk/workbench/lift/prefixes.csv")
      val prefixSource = Source.fromInputStream(prefixStream)
      val prefixLines = prefixSource.getLines

      prefixLines.map(_.split(',')).map{case Array(id, namespace) => (id, namespace.drop(1).dropRight(1))}.toMap
    }
    catch
    {
      case ex : Exception =>
      {
        logger.log(Level.WARNING, "Error loading prefix table. ", ex)
        Map.empty
      }
    }
  }
}