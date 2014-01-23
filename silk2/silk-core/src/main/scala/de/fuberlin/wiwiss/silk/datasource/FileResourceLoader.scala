package de.fuberlin.wiwiss.silk.datasource

import java.io.{File, FileInputStream, BufferedInputStream}

/**
 * A resource loader that loads files from a base directory.
 */
class FileResourceLoader(baseDir: File) extends ResourceLoader {

  /**
   * Retrieves a file by name.
   *
   * @param name The local name of the file.
   * @return An input stream for reading the file.
   *         The caller is responsible for closing the stream after reading.
   * @throws ResourceNotFoundException If no resource with the given name has been found in the base directory.
   */
  override def load(name: String) = {
    val file = new File(baseDir + "/" + name)
    if(!file.exists) throw new ResourceNotFoundException(s"Resource $name not found in directory $baseDir")
    new BufferedInputStream(new FileInputStream(file))
  }
}
