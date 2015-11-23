package org.silkframework.runtime.plugin

trait PluginModule {

  def pluginClasses: Seq[Class[_]]

}
