package org.silkframework.util

import java.io.File

/**
  * File based utils for tests
  */
object TestFileUtils {
  /**
    * Creates a temp directory and deletes it on exit.
    * @param baseName base name of the directory
    * @param suffix suffix of the directory
    * @return
    */
  def createTempDirectoryForTest(baseName: String, suffix: String, deleteOnExit: Boolean = true): File = {
    val file = File.createTempFile(baseName, suffix)
    file.delete()
    org.apache.commons.io.FileUtils.forceMkdir(file)
    if(deleteOnExit) {
      org.apache.commons.io.FileUtils.forceDeleteOnExit(file)
    }
    file
  }
}
