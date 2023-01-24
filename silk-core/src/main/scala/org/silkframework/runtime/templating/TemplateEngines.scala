package org.silkframework.runtime.templating

import org.silkframework.runtime.plugin.{PluginContext, PluginRegistry}

/**
  * Manages available template engines.
  */
object TemplateEngines {

  /**
    * Returns a list of all available template engines.
    */
  def availableEngines: Set[String] = {
    PluginRegistry.availablePlugins[TemplateEngine].map(_.id.toString).toSet
  }

  /**
    * Creates a new template engine.
    *
    * @param id The id of the engine
    */
  def create(id: String): TemplateEngine = {
    implicit val pluginContext: PluginContext = PluginContext.empty
    PluginRegistry.create[TemplateEngine](id)
  }

}
