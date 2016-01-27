package org.silkframework.config

import java.io.File

import com.typesafe.config.ConfigFactory

/**
 * Holds the configuration properties
 */
object Config {

  // Overwrite default logging pattern for java.util.logging
  if(System.getProperty("java.util.logging.SimpleFormatter.format") == null) {
    System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tb %1$td, %1$tY %1$tl:%1$tM:%1$tS %1$Tp %3$s%n%4$s: %5$s%6$s%n")
  }

  private lazy val config = {
    var fullConfig = ConfigFactory.load()
    // Check if we are running as part of the eccenca Linked Data Suite
    if (fullConfig.hasPath("elds.home")) {
      val eldsHome = fullConfig.getString("elds.home")
      val eldsConfig = ConfigFactory.parseFile(new File(eldsHome + "/etc/dataintegration/dataintegration.conf"))
      fullConfig = eldsConfig.withFallback(fullConfig)
    }
    // Check if we are running as part of the Play Framework
    val playConfig = new File(System.getProperty("user.home") + "/conf/reference.conf")
    if(playConfig.exists()) {
      fullConfig = ConfigFactory.parseFile(playConfig).withFallback(fullConfig)
    }
    fullConfig.resolve()
  }

  def apply() = config

}
