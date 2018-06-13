package org.silkframework.plugins.dataset.json

import java.net.URLEncoder
import java.util.logging.{Level, Logger}

import org.silkframework.dataset.{DataSource, PeakDataSource}
import org.silkframework.entity._
import org.silkframework.runtime.resource.Resource
import org.silkframework.util.Uri

import scala.io.Codec

/**
 * A data source that retrieves all entities from an JSON file.
 *
 * @param file JSON resource
 * @param basePath The path to the elements to be read, starting from the root element, e.g., '/Persons/Person'.
 *                 If left empty, all direct children of the root element will be read.
 * @param uriPattern A URI pattern, e.g., http://namespace.org/{ID}, where {path} may contain relative paths to elements
 */
class JsonSource(file: Resource, basePath: String, uriPattern: String, codec: Codec) extends DataSource with PeakDataSource {

  private val logger = Logger.getLogger(getClass.getName)

  private val uriRegex = "\\{([^\\}]+)\\}".r

  def retrieve(entitySchema: EntitySchema, limit: Option[Int] = None): Traversable[Entity] = {
    logger.log(Level.FINE, "Retrieving data from JSON.")
    val jsonTraverser = JsonTraverser(file)(codec)
    val selectedElements = jsonTraverser.select(basePathParts)
    val subPath = Path.parse(entitySchema.typeUri.uri) ++ entitySchema.subPath
    val subPathElements = if(subPath.operators.nonEmpty) {
      selectedElements.flatMap(_.select(subPath.operators))
    } else { selectedElements }
    new Entities(subPathElements, entitySchema, Set.empty)
  }

  private def basePathParts: Array[String] = {
    val pureBasePath = basePath.stripPrefix("/").trim
    if (pureBasePath == "") {
      Array.empty[String]
    } else {
      pureBasePath.split('/')
    }
  }

  def retrieveByUri(entitySchema: EntitySchema, entities: Seq[Uri]): Seq[Entity] = {
    logger.log(Level.FINE, "Retrieving data from JSON.")
    val jsonTraverser = JsonTraverser(file)(codec)
    val selectedElements = jsonTraverser.select(basePathParts)
    new Entities(selectedElements, entitySchema, entities.map(_.uri).toSet).toSeq
  }

  /**
   * Retrieves the most frequent paths in this source.
   */
  override def retrievePaths(t: Uri, depth: Int, limit: Option[Int]): IndexedSeq[Path] = {
    retrieveJsonPaths(t, depth, limit, leafPathsOnly = false, innerPathsOnly = false)
  }

  def retrieveJsonPaths(t: Uri, depth: Int, limit: Option[Int], leafPathsOnly: Boolean, innerPathsOnly: Boolean): IndexedSeq[Path] = {
    val json = JsonTraverser(file)(codec)
    val selectedElements = json.select(basePathParts)
    val subSelectedElements = selectedElements.flatMap(_.select(Path.parse(t.uri).operators))
    for (element <- subSelectedElements.headOption.toIndexedSeq; // At the moment, we only retrieve the path from the first found element
         path <- element.collectPaths(path = Nil, leafPathsOnly = leafPathsOnly, innerPathsOnly = innerPathsOnly, depth = depth) if path.nonEmpty) yield {
      Path(path.toList)
    }
  }

  override def retrieveTypes(limit: Option[Int]): Traversable[(String, Double)] = {
    if(file.nonEmpty) {
      val json = JsonTraverser(file)(codec)
      val selectedElements = json.select(basePathParts)
      (for (element <- selectedElements.headOption.toIndexedSeq; // At the moment, we only retrieve the path from the first found element
            path <- element.collectPaths(path = Nil, leafPathsOnly = false, innerPathsOnly = true, depth = Int.MaxValue)) yield {
        Path(path.toList).normalizedSerialization
      }) map (p => (p, 1.0))
    } else {
      Traversable.empty
    }
  }

  private class Entities(elements: Seq[JsonTraverser], entityDesc: EntitySchema, allowedUris: Set[String]) extends Traversable[Entity] {
    def foreach[U](f: Entity => U) {
      // Enumerate entities
      for ((node, index) <- elements.zipWithIndex) {
        // Generate URI
        val uri =
          if (uriPattern.isEmpty) {
            index.toString
          } else {
            uriRegex.replaceAllIn(uriPattern, m => {
              val path = Path.parse(m.group(1))
              val string = node.evaluate(path).mkString
              URLEncoder.encode(string, "UTF8")
            })
          }

        // Check if this URI should be extracted
        if (allowedUris.isEmpty || allowedUris.contains(uri)) {
          // Extract values
          val values = for (path <- entityDesc.typedPaths) yield node.evaluate(path)
          f(Entity(uri, values, entityDesc))
        }
      }
    }
  }

  override def peak(entitySchema: EntitySchema, limit: Int): Traversable[Entity] = {
    peakWithMaximumFileSize(file, entitySchema, limit)
  }
}
