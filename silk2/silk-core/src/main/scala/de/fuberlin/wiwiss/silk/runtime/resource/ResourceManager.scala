package de.fuberlin.wiwiss.silk.runtime.resource

/**
 * Reads and writes resources.
 */
trait ResourceManager extends ResourceLoader with ResourceWriter {

  override def child(name: String): ResourceManager
}
