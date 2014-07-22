package de.fuberlin.wiwiss.silk.runtime.resource

import java.io._

/**
 * A resource manager that loads files from a base directory.
 */
class FileResourceManager(baseDir: File) extends ResourceManager {

  /**
   * Lists all files in the resources directory.
   */
  override def list = {
    val files = baseDir.listFiles
    if(files != null)
      files.toList.filter(_.isFile).map(_.getName)
    else
      Nil
  }

  /**
   * Retrieves a file by name.
   *
   * @param name The local name of the file.
   * @return The file resource.
   * @throws ResourceNotFoundException If no resource with the given name has been found in the base directory.
   */
  override def get(name: String): Resource = {
    // We still need to support the deprecated method of using absolute paths
    val oldAbsoluteFile = new File(name)
    // We still need to support the deprecated method of putting files in a dataset directory in the user home
    val oldLocalFile = new File(System.getProperty("user.home") + "/.silk/datasets/" + name)
    // Current method of searching for files in the configured base dir
    val newFile = new File(baseDir, name)

    // Try to find the file in all locations
    val file =
      if(newFile.exists)
        newFile
      else if(oldAbsoluteFile.isAbsolute && oldAbsoluteFile.exists)
        oldAbsoluteFile
      else if(oldLocalFile.exists)
        oldLocalFile
      else
        throw new ResourceNotFoundException(s"Resource $name not found in directory $baseDir")

    new FileResource(name, file)
  }

  override def put(name: String)(write: OutputStream => Unit) {
    baseDir.mkdirs()
    val outputStream = new BufferedOutputStream(new FileOutputStream(baseDir + "/" + name))
    write(outputStream)
    outputStream.close()
  }

  override def delete(name: String) {
    def deleteRecursive(file: File): Unit = {
      if (file.isDirectory) {
        file.listFiles.foreach(deleteRecursive)
      }
      if (file.exists && !file.delete()) throw new IOException("Could not delete file " + file)
    }
    deleteRecursive(new File(baseDir + "/" + name))
  }

  override def listChildren: List[String] = {
    val files = baseDir.listFiles
    if(files != null)
      files.toList.filter(_.isDirectory).map(_.getName)
    else
      Nil
  }

  override def child(name: String): ResourceManager = {
    new FileResourceManager(new File(baseDir + "/" + name))
  }

  override def parent: Option[ResourceManager] = {
    for(parent <- Option(baseDir.getParentFile)) yield
      new FileResourceManager(parent)
  }
}

private class FileResource(val name: String, file: File) extends Resource {

  override def load = {
    new BufferedInputStream(new FileInputStream(file))
  }

}
