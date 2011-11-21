/* 
 * Copyright 2009-2011 Freie Universität Berlin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.fuberlin.wiwiss.silk.util

import java.io._
import xml.{PrettyPrinter, NodeSeq}

/**
 * Defines additional methods on XML, which are missing in the standard library.
 */
object XMLUtils {
  implicit def toXMLUtils(xml: NodeSeq) = new XMLUtils(xml)
}

/**
 * Defines additional methods on XML, which are missing in the standard library.
 */
class XMLUtils(xml: NodeSeq) {
  def toFormattedString = {
    val stringWriter = new StringWriter()
    write(stringWriter)
    stringWriter.toString
  }

  def write(file: File) {
    val fileWriter = new OutputStreamWriter(new FileOutputStream(file), "UTF-8")
    try {
      write(fileWriter)
    }
    finally {
      fileWriter.close()
    }
  }

  def write(writer: Writer) {
    val printer = new PrettyPrinter(Int.MaxValue, 2)

    writer.write(printer.formatNodes(xml))
    writer.write("\n")
    writer.flush()
  }
}
