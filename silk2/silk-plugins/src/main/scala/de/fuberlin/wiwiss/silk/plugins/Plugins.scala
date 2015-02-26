package de.fuberlin.wiwiss.silk.plugins

import java.io.File
import java.util.logging.{Level, Logger}
import de.fuberlin.wiwiss.silk.dataset.DatasetPlugin
import de.fuberlin.wiwiss.silk.linkagerule.input.Transformer
import de.fuberlin.wiwiss.silk.linkagerule.similarity.{Aggregator, DistanceMeasure}
import de.fuberlin.wiwiss.silk.plugins.dataset.{JsonPlugins, JenaPlugins}
import de.fuberlin.wiwiss.silk.util.Timer
;

/**
 * Registers all default plugins as well as external plugins found in the provided directory.
 */
object Plugins {
  /** Indicates if register() has already been called */
  private var registered = false

  private implicit val logger = Logger.getLogger(Plugins.getClass.getName)

  /**
   * Registers all default plugins as well as external plugins found in the provided directory.
   */
  def register(pluginsDir: File = new File(System.getProperty("user.home") + "/.silk/plugins/")): Unit = synchronized {

    logger.log(Level.FINE, "Registering plugins.")

    if(!registered) {
      CorePlugins.register()
      JenaPlugins.register()
      JsonPlugins.register()
      registerExternalPlugins(pluginsDir)
      registered = true
    }
  }

  /**
   * Registers external plugins.
   */
  private def registerExternalPlugins(pluginsDir: File) {
    logger.log(Level.FINE, "Registering 3rd party plugins.")

    Timer("Registering external plugins") {
      if(pluginsDir.isDirectory) {
        DatasetPlugin.registerJars(pluginsDir)
        Transformer.registerJars(pluginsDir)
        DistanceMeasure.registerJars(pluginsDir)
        Aggregator.registerJars(pluginsDir)
      }
      else {
        logger.info("No plugins loaded because the plugin directory " + pluginsDir + " has not been found.")
      }
    }
  }
}
