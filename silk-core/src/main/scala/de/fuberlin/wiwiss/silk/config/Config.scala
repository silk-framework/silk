package de.fuberlin.wiwiss.silk.config

import com.typesafe.config.ConfigFactory

/**
 * Holds the configuration properties
 */
object Config {

  private lazy val config = ConfigFactory.load()

  def apply() = config

}
