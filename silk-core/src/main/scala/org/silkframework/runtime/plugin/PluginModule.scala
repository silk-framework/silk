package org.silkframework.runtime.plugin

trait PluginModule {

  def pluginClasses: Seq[Class[_ <: AnyPlugin]]

  def load(): Unit = {}

  def unload(): Unit = {}

}
