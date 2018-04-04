package org.silkframework.runtime.resource

import java.io.InputStream
import java.time.Instant

/**
  * A resource in the classpath.
  *
  * @param path The path of the resource, e.g., "org/silkframework/resource.txt"
  */
case class ClasspathResource(path: String) extends Resource {

  val name: String = path.split(',').last

  def exists: Boolean = {
    Option(getClass.getClassLoader.getResourceAsStream(path)).isDefined
  }

  def size: Option[Long] = {
    val length = loadAsBytes.length
    Some(length)
  }

  def modificationTime: Option[Instant] = None

  override def inputStream: InputStream = {
    val inputStream = getClass.getClassLoader.getResourceAsStream(path)
    if(inputStream == null) {
      throw new ResourceNotFoundException(s"No resource found at classpath '$path'.")
    }
    inputStream
  }
}


