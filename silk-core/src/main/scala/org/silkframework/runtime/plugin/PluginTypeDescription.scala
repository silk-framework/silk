package org.silkframework.runtime.plugin

/**
  * Describes a plugin type.
  *
  * @param baseClass The base class of all plugins of this type. For instance, 'DistanceMeasure' for distance measure plugins.
  * @param customDescription Generates additional metadata for each plugin of this type.
  */
case class PluginTypeDescription(baseClass: Class[_], customDescription: CustomPluginDescriptionGenerator = new NoCustomPluginDescription) {

  def name: String = baseClass.getName

}

//TODO document
trait CustomPluginDescriptionGenerator {

  def generate(pluginClass: Class[_]): Option[CustomPluginDescription]

}

class NoCustomPluginDescription extends CustomPluginDescriptionGenerator {

  override def generate(pluginClass: Class[_]): Option[CustomPluginDescription] = None

}

trait CustomPluginDescription {

  /**
    * Generates additional documentation for a given plugin.
    */
  def generateDocumentation(sb: StringBuilder): Unit

}

