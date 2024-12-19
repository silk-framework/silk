package org.silkframework.util

import java.io.File

import scala.util.Try

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

  /** Creates a temp directory that will be deleted at the end of the code block. */
  def withTempDirectory[T](block: File => T, baseName: String = "tempDirectory", suffix: String = ""): T = {
    val tempDir = createTempDirectoryForTest(baseName, suffix)
    try {
      block(tempDir)
    } finally {
      org.apache.commons.io.FileUtils.forceDelete(tempDir)
    }
  }

  /** Executes a block of code with a temporary file that will be deleted after the block has finished executing or failed. */
  def withTempFile[T](block: File => T,
                      baseName: String = "tempFile",
                      suffix: String = ""): T = {
    val tempFile = File.createTempFile(baseName, suffix)
    tempFile.deleteOnExit()
    try {
      block(tempFile)
    } finally {
      Try(tempFile.delete())
    }
  }
}
