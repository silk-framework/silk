package org.silkframework.rule.evaluation

import java.io.{File, InputStream}

import org.silkframework.entity.Link

import scala.io.Source
import scala.xml.{Node, XML}

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
    new ReferenceLinks(readLinks(xml, "=").toSet, readLinks(xml, "!=").toSet, Set.empty)
  }

  private def readLinks(xml: Node, relation: String): Traversable[Link] = {
    for (cell <- xml \ "Alignment" \ "map" \ "Cell" if (cell \ "relation").text == relation) yield {
      new Link(
        source = (cell \ "entity1" \ "@{http://www.w3.org/1999/02/22-rdf-syntax-ns#}resource").text,
        target = (cell \ "entity2" \ "@{http://www.w3.org/1999/02/22-rdf-syntax-ns#}resource").text,
        confidence = Some((cell \ "measure").text.toDouble)
      )
    }
  }

  private val NTriplesRegex = """<([^>]*)>\s*<([^>]*)>\s*<([^>]*)>\s*\.\s*""".r

  /**
   * Reads reference links from an N-Triples source.
   *
   * @param source The N-Triples source
   * @param positivePredicate The predicate URI of the positive reference links i.e. http://www.w3.org/2002/07/owl#sameAs
   * @return The loaded reference links
   */
  def readNTriples(source: Source, positivePredicate: String = "http://www.w3.org/2002/07/owl#sameAs"): ReferenceLinks = {
    val positiveLinks =
      for (NTriplesRegex(sourceUri, predicateUri, targetUri) <- source.getLines() if predicateUri == positivePredicate) yield {
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
