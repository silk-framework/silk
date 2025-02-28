package org.silkframework.runtime.plugin

/**
  * A plugin action.
  *
  * Plugin actions provide additional functionality besides the default execution.
  * They can be triggered from the plugin UI.
  * Each action is based on a method on the plugin class.
  * The method can have one optional parameter of type PluginContext.
  * The return value of the method will be converted to a string and displayed in the UI.
  * The string may use Markdown formatting.
  * The method may return an Option, in which case None will result in no message being displayed.
  * It may raise an exception to signal an error to the user.
  */
trait PluginAction {

  /** The label of the action. */
  def label: String

  /** The description of the action. */
  def description: String

  /** The icon of the action. */
  def icon: Option[String]

  /** Executes the action. */
  def apply(plugin: AnyRef)(implicit pluginContext: PluginContext): Option[String]
}
