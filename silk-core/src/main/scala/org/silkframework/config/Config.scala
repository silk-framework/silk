package org.silkframework.config

import java.io.File

import com.typesafe.config.ConfigFactory

/**
 * Holds the configuration properties
 */
object Config {

  private lazy val config = {
    var fullConfig = ConfigFactory.load()
    // Check if we are running as part of the eccenca Linked Data Suite
    if (fullConfig.hasPath("elds.home")) {
      val eldsHome = fullConfig.getString("elds.home")
      val eldsConfig = ConfigFactory.parseFile(new File(eldsHome + "/etc/dataintegration/dataintegration.conf"))
      fullConfig = eldsConfig.withFallback(fullConfig)
    }
    // Check if we are running as part of the Play Framework
    val playConfig = new File(System.getProperty("user.home") + "/conf/application.conf")
    if(playConfig.exists()) {
      fullConfig = ConfigFactory.parseFile(playConfig).withFallback(fullConfig)
    }
    fullConfig.resolve()
  }

  def apply() = config

}
