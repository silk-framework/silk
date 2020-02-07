package org.silkframework.runtime.resource

import java.io.{File, InputStream}
import java.time.Instant

/**
  * A resource in the classpath.
  *
  * @param resourcePath The path of the resource, e.g., "org/silkframework/resource.txt"
  */
case class ClasspathResource(resourcePath: String) extends Resource {

  override def path: String = {
    try {
      if (! new File(resourcePath).exists) {
        asFilePath.replaceAllLiterally("%20"," ")
      }
      else {
        resourcePath
      }
    }
    catch {
      case _: Throwable => asFilePath
    }
  }

  val name: String = resourcePath.split(',').last

  def exists: Boolean = {
    Option(getClass.getClassLoader.getResourceAsStream(resourcePath)).isDefined
  }

  def size: Option[Long] = None

  def modificationTime: Option[Instant] = None

  override def inputStream: InputStream = {
    val inputStream = getClass.getClassLoader.getResourceAsStream(resourcePath)
    if(inputStream == null) {
      throw new ResourceNotFoundException(s"No resource found at classpath '$resourcePath'.")
    }
    inputStream
  }


  /**
    * Returns a file path. Sometimes the Classloader does not return a valid path in non production modes.
    * In these situations a fallback to this path occurs.
    *
    * @return Path to module ./target/test-classes folder with appended resourcePath
    */
  def asFilePath: String = asFileResource.path


  /**
    * Returns a file path. Sometimes the Classloader does not return a valid path in non production modes.
    * In these situations a fallback to this path occurs.
    *
    * @return Resource in module ./target/test-classes folder with appended resourcePath
    */
  def asFileResource: WritableResource = FileResource(new File(getClass.getResource(s"/$resourcePath").getPath))

}
