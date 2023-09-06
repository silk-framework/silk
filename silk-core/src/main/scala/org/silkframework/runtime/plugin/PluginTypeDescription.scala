package org.silkframework.runtime.plugin

import org.silkframework.runtime.plugin.annotations.PluginType
import org.silkframework.util.StringUtils.toStringUtils

/**
  * Describes a plugin type.
  *
  * @param baseClass The base class of all plugins of this type. For instance, 'DistanceMeasure' for distance measure plugins.
  * @param name Unique name of this plugin type.
  * @param label Human-readably label for this plugin type.
  * @param customDescription Generates additional metadata for each plugin of this type.
  */
case class PluginTypeDescription(baseClass: Class[_], name: String, label: String, customDescription: CustomPluginDescriptionGenerator = new NoCustomPluginDescription)

object PluginTypeDescription {

  def forClassOpt(pluginClass: Class[_]): Option[PluginTypeDescription] = {
    val typeAnnotations = pluginClass.getAnnotationsByType(classOf[PluginType])
    if (typeAnnotations.length > 1) {
      throw new IllegalArgumentException(s"Class ${pluginClass.getName} has multiple ${classOf[PluginType].getName} annotations.")
    } else {
      for (typeAnnotation <- typeAnnotations.headOption) yield {
        val name = pluginClass.getName
        val label = if (typeAnnotation.label().nonEmpty) typeAnnotation.label() else pluginClass.getSimpleName.toSentenceCase
        val customDescriptionGenerator = typeAnnotation.customDescription().getDeclaredConstructor().newInstance()
        PluginTypeDescription(pluginClass, name, label, customDescriptionGenerator)
      }
    }
  }

  def forClass(pluginClass: Class[_]): PluginTypeDescription = {
    forClassOpt(pluginClass).getOrElse(throw new IllegalArgumentException(s"Class ${pluginClass.getName} is missing a ${classOf[PluginType].getName} annotation."))
  }

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

}
