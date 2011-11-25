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

package de.fuberlin.wiwiss.silk.plugins.writer

import de.fuberlin.wiwiss.silk.output.{Formatter, LinkWriter}
import de.fuberlin.wiwiss.silk.util.plugin.Plugin
import de.fuberlin.wiwiss.silk.entity.Link
import java.io.{File, BufferedWriter, Writer}

/**
 * A file writer.
 */
@Plugin(id = "file", label = "File", description = "Writes the links to a file. Links are written to {user.dir}/.silk/output/ by default.")
case class FileWriter(file: String = "output.nt", format: String = "ntriples") extends LinkWriter {
  private val formatter = Formatter(format)

  private var out: Writer = null

  override def open() {
    //Translate relative paths to absolute paths
    val filePath = if(new File(file).isAbsolute) file else System.getProperty("user.home") + "/.silk/output/" + file
    //Create parent directory
    new File(filePath).getParentFile.mkdirs()
    //Create buffered writer
    out = new BufferedWriter(new java.io.FileWriter(filePath))
    //Write header
    out.write(formatter.header)
  }

  override def write(link: Link, predicateUri: String) {
    out.write(formatter.format(link, predicateUri))
  }

  override def close() {
    if (out != null) {
      out.write(formatter.footer)
      out.close()
      out = null
    }
  }
}
