package org.silkframework.runtime.resource

import java.io.InputStream
import java.time.Instant

/**
  * A resource in the classpath.
  *
  * @param p The path of the resource, e.g., "org/silkframework/resource.txt"
  */
case class ClasspathResource(p: String) extends Resource {

  val name: String = p.split(',').last

  lazy val fileLocation: String = Option(getClass.getClassLoader.getResource(p))
    .map(u => u.getFile).getOrElse(throwRnfException)

  def exists: Boolean = {
    Option(getClass.getClassLoader.getResourceAsStream(p)).isDefined
  }

  def size: Option[Long] = {
    val length = loadAsBytes.length
    Some(length)
  }

  def modificationTime: Option[Instant] = None

  override def inputStream: InputStream = {
    val inputStream = getClass.getClassLoader.getResourceAsStream(p)
    if(inputStream == null)
      throwRnfException
    inputStream
  }

  private def throwRnfException = throw new ResourceNotFoundException(s"No resource found at classpath '$p'.")

  /**
    * The path of this resource.
    */
  override def path: String = fileLocation
}
