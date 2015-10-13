package de.fuberlin.wiwiss.silk.runtime.plugin

trait PluginModule {

  def pluginClasses: Seq[Class[_]]

}
