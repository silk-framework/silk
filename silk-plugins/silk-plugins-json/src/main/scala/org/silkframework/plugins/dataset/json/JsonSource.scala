package org.silkframework.plugins.dataset.json

import java.net.URLEncoder
import java.util.logging.{Level, Logger}
import org.silkframework.dataset.DataSource
import org.silkframework.entity._
import org.silkframework.entity.rdf.{SparqlRestriction, SparqlEntitySchema}
import org.silkframework.runtime.resource.Resource
import JsonParser._
import play.api.libs.json._

import scala.io.Codec

/**
 * A data source that retrieves all entities from an JSON file.
 *
 * @param file JSON resource
 * @param basePath The path to the elements to be read, starting from the root element, e.g., '/Persons/Person'.
 *                 If left empty, all direct children of the root element will be read.
 * @param uriPattern A URI pattern, e.g., http://namespace.org/{ID}, where {path} may contain relative paths to elements
 */
class JsonSource(file: Resource, basePath: String, uriPattern: String, codec: Codec) extends DataSource {

  private val logger = Logger.getLogger(getClass.getName)

  private val uriRegex = "\\{([^\\}]+)\\}".r

  /**
   * Retrieves entities from this source which satisfy a specific entity description.
   */
  override def retrieveSparqlEntities(entityDesc: SparqlEntitySchema, entities: Seq[String] = Seq.empty): Traversable[Entity] = {
    logger.log(Level.FINE, "Retrieving data from JSON.")
    val json = load(file)(codec)
    val selectedElements = select(json, basePath.stripPrefix("/").split('/'))
    new Entities(selectedElements, entityDesc, entities.toSet)

  }

  /**
   * Retrieves the most frequent paths in this source.
   */
  override def retrieveSparqlPaths(restriction: SparqlRestriction, depth: Int, limit: Option[Int]): Traversable[(Path, Double)] = {
    val json = load(file)(codec)
    val selectedElements = select(json, basePath.stripPrefix("/").split('/'))
    for (element <- selectedElements.headOption.toSeq; // At the moment, we only retrieve the path from the first found element
         path <- collectPaths(element)) yield {
      (Path(restriction.variable, path.toList), 1.0)
    }
  }

  private class Entities(elements: Seq[JsValue], entityDesc: SparqlEntitySchema, allowedUris: Set[String]) extends Traversable[Entity] {
    def foreach[U](f: Entity => U) {
      // Enumerate entities
      for ((node, index) <- elements.zipWithIndex) {
        // Generate URI
        val uri =
          if (uriPattern.isEmpty)
            index.toString
          else
            uriRegex.replaceAllIn(uriPattern, m => {
              val path = Path.parse(m.group(1))
              val string = evaluate(node, path).mkString
              URLEncoder.encode(string, "UTF8")
            })

        // Check if this URI should be extracted
        if(allowedUris.isEmpty || allowedUris.contains(uri)) {
          // Extract values
          val values = for (path <- entityDesc.paths) yield evaluate(node, path)
          f(new Entity(uri, values, entityDesc))
        }
      }
    }
  }

}
