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

package de.fuberlin.wiwiss.silk.evaluation

import io.Source
import java.io.{InputStream, File}
import xml.{Node, XML}
import de.fuberlin.wiwiss.silk.entity.Link

/**
 * Reads the alignment format specified at http://alignapi.gforge.inria.fr/format.html.
 */
object ReferenceLinksReader {
  def read(file: File): ReferenceLinks = {
    file.getName.split('.').last match {
      case "nt" => readNTriples(Source.fromFile(file))
      case "n3debug" => readN3Debug(Source.fromFile(file))
      case "xml" | "rdf" => readReferenceLinks(file)
      case format => throw new IllegalArgumentException("Unsupported format: " + format)
    }
  }

  def readReferenceLinks(inputStream: InputStream): ReferenceLinks = {
    readReferenceLinks(XML.load(inputStream))
  }

  def readReferenceLinks(file: File): ReferenceLinks = {
    readReferenceLinks(XML.loadFile(file))
  }

  def readReferenceLinks(xml: Node): ReferenceLinks = {
    new ReferenceLinks(readLinks(xml, "=").toSet, readLinks(xml, "!=").toSet)
  }

  private def readLinks(xml: Node, relation: String): Traversable[Link] = {
    for (cell <- xml \ "Alignment" \ "map" \ "Cell" if (cell \ "relation" text) == relation) yield {
      new Link(
        source = cell \ "entity1" \ "@{http://www.w3.org/1999/02/22-rdf-syntax-ns#}resource" text,
        target = cell \ "entity2" \ "@{http://www.w3.org/1999/02/22-rdf-syntax-ns#}resource" text,
        confidence = Some((cell \ "measure").text.toDouble)
      )
    }
  }

  private val NTriplesRegex = """<([^>]*)>\s*<([^>]*)>\s*<([^>]*)>\s*\.\s*""".r

  def readNTriples(source: Source): ReferenceLinks = {
    val positiveLinks =
      for (NTriplesRegex(sourceUri, predicateUri, targetUri) <- source.getLines() if predicateUri == "http://www.w3.org/2002/07/owl#sameAs") yield {
        new Link(sourceUri, targetUri)
      }

    new ReferenceLinks(positiveLinks.toSet, Set.empty)
  }

  private val N3DebugRegex = """\[(\d*\.\d*)\]:\s*<([^>]*)>\s*<[^>]*>\s*<([^>]*)>\s*\.\s*""".r

  def readN3Debug(source: Source): ReferenceLinks = {
    val positiveLinks =
      for (N3DebugRegex(confidence, sourceUri, targetUri) <- source.getLines()) yield {
        new Link(sourceUri, targetUri, Some(confidence.toDouble))
      }

    new ReferenceLinks(positiveLinks.toSet, Set.empty)
  }
}
