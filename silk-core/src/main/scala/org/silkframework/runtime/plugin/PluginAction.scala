package org.silkframework.runtime.plugin

/**
  * A plugin action.
  */
trait PluginAction {

  /** The label of the action. */
  def label: String

  /** The description of the action. */
  def description: String

  /** The icon of the action. */
  def icon: Option[String]

  /** Executes the action. */
  def call(plugin: AnyRef)(implicit pluginContext: PluginContext): String
}
