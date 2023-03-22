package org.silkframework.runtime.resource

import java.io.{File, InputStream}
import java.time.Instant

/**
  * A resource in the classpath.
  *
  * @param path The path of the resource, e.g., "org/silkframework/resource.txt"
  */
case class ClasspathResource(path: String) extends Resource {

  val name: String = path.split('/').last

  def exists: Boolean = {
    Option(getClass.getClassLoader.getResourceAsStream(path)).isDefined
  }

  def size: Option[Long] = {
    val stream = inputStream
    try {
      var count = 0L
      while (stream.read() != -1) {
        count += 1
      }
      Some(count)
    } finally {
      stream.close()
    }
  }

  def modificationTime: Option[Instant] = None

  override def inputStream: InputStream = {
    val inputStream = getClass.getClassLoader.getResourceAsStream(path)
    if(inputStream == null) {
      throw new ResourceNotFoundException(s"No resource found at classpath '$path'.")
    }
    inputStream
  }

  /**
    * Returns a file path. Sometimes the Classloader does not return a valid path in non production modes.
    * In these situations a fallback to this path occurs.
    *
    * @return Resource in module ./target/test-classes folder with appended resourcePath
    */
  def asFileResource: WritableResource = FileResource(new File(getClass.getClassLoader.getResource(s"/$path").getPath))

}
