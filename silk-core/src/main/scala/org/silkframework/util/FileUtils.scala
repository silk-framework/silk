/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.silkframework.util

import org.silkframework.config.DefaultConfig

import java.io.{File, IOException}
import java.nio.file.Files
import scala.language.implicitConversions
import scala.util.Try

/**
 * Defines additional methods on Files, which are not in the standard library.
 */
object FileUtils {
  implicit def toFileUtils(file: File): FileUtils = new FileUtils(file)
  final val tmpDirKey = "config.tempFilesDirectory"

  /** Directory containing temporary files that will be removed on every application start-up. */
  lazy val tempDir: String = {
    val cfg = DefaultConfig.instance()
    def default: String = {
      val tmpDir = Files.createTempDirectory("silk-tmp-file-dir")
      tmpDir.toString
    }
    if(cfg.hasPath(tmpDirKey)) {
      Try(cfg.getString(tmpDirKey)).getOrElse(default)
    } else {
      default
    }
  }
}

/**
 * Defines additional methods on Files, which are missing in the standard library.
 */
class FileUtils(file: File) {

  /**
    * Variant of mkdirs() that throws an IOException, if the directory could not be created.
    *
    * @throws java.io.IOException if the directory or any of its sub directories could not be created
    */
  def safeMkdirs(): Unit = {
    if (!file.exists && !file.mkdirs()) {
      throw new IOException("Could not create directory at: " + file.getCanonicalPath)
    }
  }

  /**
   * Deletes this directory and all sub directories.
   *
   * @throws java.io.IOException if the directory or any of its sub directories could not be deleted
   */
  def deleteRecursive(keepBaseFile: Boolean = false): Unit = {
    if (file.isDirectory) {
      file.listFiles().foreach(child => new FileUtils(child).deleteRecursive())
    }

    if (!keepBaseFile && file.exists && !file.delete()) throw new IOException("Could not delete file " + file)
  }

  /**
    * Deletes this directory and all sub directories on shutdown.
    */
  def deleteRecursiveOnExit(): Unit = {
    Runtime.getRuntime.addShutdownHook(new Thread() {
      override def run(): Unit = {
        deleteRecursive()
      }
    })
  }

  /**
   * Adds a suffix to the file path.
   */
  def +(suffix: String): File = new File(file.toString + suffix)
}
