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

/**
  * Generates a custom plugin description that is specific to a given plugin type.
  */
trait CustomPluginDescriptionGenerator {

  def generate(pluginClass: Class[_]): Option[CustomPluginDescription]

}

/**
  * Default plugin description generator, for plugins that do not provide a custom description.
  */
class NoCustomPluginDescription extends CustomPluginDescriptionGenerator {

  override def generate(pluginClass: Class[_]): Option[CustomPluginDescription] = None

}

/**
  * Custom plugin description that is specific to a given plugin type.
  */
trait CustomPluginDescription {

  /**
    * Generates additional documentation for a particular plugin.
    */
  def generateDocumentation(sb: StringBuilder): Unit

  /**
    * Additional properties (key-value pairs) for this plugin type.
    * Should be returned in JSON representations.
    */
  def additionalProperties(): Map[String, String] = Map.empty

}
