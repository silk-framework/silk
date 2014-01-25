package de.fuberlin.wiwiss.silk.util.plugin

import java.io.InputStream

/**
 * Writes resources.
 */
trait ResourceWriter {

  def put(name: String, inputStream: InputStream)

  def delete(name: String)
}
