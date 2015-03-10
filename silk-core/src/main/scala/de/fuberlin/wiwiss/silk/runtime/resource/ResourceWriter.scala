package de.fuberlin.wiwiss.silk.runtime.resource

import java.io.{OutputStream, InputStream}

/**
 * Writes resources.
 */
trait ResourceWriter {

  def put(name: String)(write: OutputStream => Unit)

  def put(name: String, inputStream: InputStream) {
    put(name) { outputStream =>
      var b = inputStream.read()
      while(b != -1) {
        outputStream.write(b)
        b = inputStream.read()
      }
    }
  }

  def delete(name: String)
}
