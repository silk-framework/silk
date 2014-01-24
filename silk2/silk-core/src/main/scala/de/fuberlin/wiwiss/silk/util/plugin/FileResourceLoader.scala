package de.fuberlin.wiwiss.silk.util.plugin

import java.io.{File, FileInputStream, BufferedInputStream}

/**
 * A resource loader that loads files from a base directory.
 */
class FileResourceLoader(baseDir: File) extends ResourceLoader {

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
    val newFile = new File(baseDir + "/" + name)

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
}

private class FileResource(val name: String, file: File) extends Resource {

  override def load = {
    new BufferedInputStream(new FileInputStream(file))
  }

}
