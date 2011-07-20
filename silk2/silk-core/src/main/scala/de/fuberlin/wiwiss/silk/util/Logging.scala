package de.fuberlin.wiwiss.silk.util

import java.util.logging.{Level, Logger}

object Logging {
  def logQueries_=(enable: Boolean) {
    Logger.getLogger("de.fuberlin.wiwiss.silk.util.sparql").setLevel(Level.FINE)
  }
}