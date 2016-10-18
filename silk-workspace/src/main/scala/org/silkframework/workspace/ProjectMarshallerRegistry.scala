package org.silkframework.workspace

import org.silkframework.config.Prefixes
import org.silkframework.runtime.plugin.PluginRegistry
import org.silkframework.runtime.resource.EmptyResourceManager

/**
  * Holds all available project marshallers.
  * Marshallers are registered as plugins of the [[ProjectMarshallingTrait]].
  */
object ProjectMarshallerRegistry {

  /**
    * Retrieves all available marshalling plugins.
    */
  def marshallingPlugins: Seq[ProjectMarshallingTrait] = {
    implicit val prefixes = Prefixes.empty
    implicit val resources = EmptyResourceManager
    val pluginConfigs = PluginRegistry.availablePluginsForClass(classOf[ProjectMarshallingTrait])
    pluginConfigs.map(pc =>
      PluginRegistry.create[ProjectMarshallingTrait](pc.id)
    )
  }

  /**
    * Retrieves a marshaller by its plugin id.
    */
  def marshallerById(marshallerId: String): Option[ProjectMarshallingTrait] = {
    marshallingPlugins.find(_.id == marshallerId)
  }

  /**
    * Retrieves an suitable marshalller for a given filename.
    *
    * @param fileName The filename, e.g., project.zip
    * @throws IllegalArgumentException If no marshaller could be found for the given file name.
    */
  def marshallerForFile(fileName: String): ProjectMarshallingTrait = {
    val dotIndex = fileName.lastIndexOf('.')
    if (dotIndex < 0) {
      throw new IllegalArgumentException("No recognizable file name suffix in uploaded file.")
    }
    val suffix = fileName.substring(dotIndex + 1)
    val marshallers = marshallingPluginsByFileHandler()
    marshallers.get(suffix) match {
      case Some(m) => m
      case None => throw new IllegalArgumentException("No handler found for " + suffix + " files")
    }
  }

  private def marshallingPluginsByFileHandler(): Map[String, ProjectMarshallingTrait] = {
    marshallingPlugins.map { mp =>
      mp.suffix.map(s => (s, mp))
    }.flatten.toMap
  }

}
