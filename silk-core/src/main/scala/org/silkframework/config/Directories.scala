package org.silkframework.config

import java.io.File
import java.nio.file.Path

/**
  * Holds the paths to important directories used by Silk.
  *
  * @param config The directory where configuration files are stored.
  * @param data  The directory where project data is stored.
  * @param cache The directory where cache files are stored.
  * @param logs  The directory where log files are stored.
  */
case class Directories(config: Path,
                       data: Path,
                       cache: Path,
                       logs: Path)

object Directories {

  private val config: Directories = {
    val dirConfig = DefaultConfig.instance().getConfig("directories")
    Directories(
      config = new File(dirConfig.getString("config")).toPath,
      data = new File(dirConfig.getString("data")).toPath,
      cache = new File(dirConfig.getString("cache")).toPath,
      logs = new File(dirConfig.getString("logs")).toPath
    )
  }

  /**
   * Returns the configured directories.
   */
  def apply(): Directories = config

}
