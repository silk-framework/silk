package de.fuberlin.wiwiss.silk.util.plugin

/**
 * Plugin interface.
 */
trait AnyPlugin {
  private[plugin] var id = ""
  private[plugin] var parameters = Map[String, String]()

  def pluginId = id

  override def toString = {
    getClass.getSimpleName + "(" + parameters.map { case (key, value) => key + "=" + value }.mkString(" ") + ")"
  }
}
