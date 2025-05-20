package org.silkframework.runtime.resource

import java.io._

/**
 * A resource manager that loads files from a base directory.
 */
case class FileResourceManager(baseDir: File) extends ResourceManager {

  val basePath: String = baseDir.getCanonicalPath

  def this(baseDir: String) = this(new File(baseDir))

  /**
   * Lists all files in the resources directory.
   */
  override def list: List[String] = {
    val files = baseDir.listFiles
    if(files != null) {
      files.toList.filter(_.isFile).map(_.getName)
    } else {
      Nil
    }
  }

  /**
   * Retrieves a file by name.
   *
   * @param name The local name of the file.
   * @return The file resource.
   * @throws ResourceNotFoundException If no resource with the given name has been found in the base directory.
   */
  override def get(name: String, mustExist: Boolean): WritableResource = {
    // Get the file in the configured base dir
    val newFile = new File(baseDir, name)
    if(!newFile.getCanonicalPath.startsWith(baseDir.getCanonicalPath)) {
      throw new IllegalArgumentException("Illegal file resource access: '" + name +
        "'. Requesting resources outside of the resource base directory is not permitted.")
    }

    // Check if the file exists
    val file =
      if(!newFile.exists && mustExist) {
        throw new ResourceNotFoundException(s"Resource $name not found in directory $baseDir")
      } else {
        newFile
      }

    FileResource(file)
  }

  override def delete(name: String): Unit = {
    def deleteRecursive(file: File): Unit = {
      if (file.isDirectory) {
        file.listFiles.foreach(deleteRecursive)
      }
      if (file.exists && !file.delete()) {
        throw new IOException("Could not delete file " + file)
      }
    }
    deleteRecursive(new File(baseDir, name))
  }

  override def listChildren: List[String] = {
    val files = baseDir.listFiles
    if(files != null) {
      files.toList.filter(_.isDirectory).map(_.getName)
    } else {
      Nil
    }
  }

  override def child(name: String): ResourceManager = {
    val newDir = new File(baseDir, name)
    if(!newDir.getCanonicalPath.startsWith(baseDir.getCanonicalPath)) {
      throw new IllegalArgumentException("Illegal resource directory access: '" + name +
        "'. Requesting resources outside of the resource base directory is not permitted.")
    }
    FileResourceManager(newDir)
  }

  override def parent: Option[ResourceManager] = {
    for(parent <- Option(baseDir.getAbsoluteFile.getParentFile)) yield
      FileResourceManager(parent)
  }
}


