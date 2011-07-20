package de.fuberlin.wiwiss.silk.util

import java.io.{IOException, File}

/**
 * Defines additional methods on Files, which are not in the standard library.
 */
object FileUtils {
  implicit def toFileUtils(file: File) = new FileUtils(file)
}

/**
 * Defines additional methods on Files, which are missing in the standard library.
 */
class FileUtils(file: File) {
  /**
   * Deletes this directory and all sub directories.
   *
   * @throws IOException if the directory or any of its sub directories could not be deleted
   */
  def deleteRecursive(): Unit = {
    if (file.isDirectory) {
      file.listFiles().foreach(child => new FileUtils(child).deleteRecursive())
    }

    if (file.exists && !file.delete()) throw new IOException("Could not delete file " + file)
  }

  /**
   * Adds a suffix to the file path.
   */
  def +(suffix: String) = new File(file + suffix)
}
