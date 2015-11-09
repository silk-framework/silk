package de.fuberlin.wiwiss.silk.runtime.resource

import java.io.{InputStream, OutputStream}

trait WritableResource extends Resource{

  def write(write: OutputStream => Unit)

  def write(inputStream: InputStream) {
    write { outputStream =>
      var b = inputStream.read()
      while(b != -1) {
        outputStream.write(b)
        b = inputStream.read()
      }
    }
  }

  def write(content: String): Unit = {
    write(os => os.write(content.getBytes))
  }

}
