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

import java.io.{IOException, File}
import scala.language.implicitConversions

/**
 * Defines additional methods on Files, which are not in the standard library.
 */
object FileUtils {
  implicit def toFileUtils(file: File): FileUtils = new FileUtils(file)
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
