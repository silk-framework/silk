package org.silkframework.runtime.plugin

trait PluginModule {

  def pluginClasses: Seq[Class[_]]

  def load(): Unit = {}

  def unload(): Unit = {}

}
