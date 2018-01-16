package org.silkframework.plugins.dataset.json

import java.net.URLEncoder
import java.util.logging.{Level, Logger}

import org.silkframework.dataset._
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
class JsonSource(file: Resource, basePath: String, uriPattern: String, codec: Codec) extends DataSource with PeakDataSource with SchemaExtractionSource {

  private val logger = Logger.getLogger(getClass.getName)

  private val uriRegex = "\\{([^\\}]+)\\}".r

  def retrieve(entitySchema: EntitySchema, limit: Option[Int] = None): Traversable[Entity] = {
    logger.log(Level.FINE, "Retrieving data from JSON.")
    val jsonTraverser = JsonTraverser(file)(codec)
    val selectedElements = jsonTraverser.select(basePathParts)
    val subPath = entitySchema.subPath ++ Path.parse(entitySchema.typeUri.uri)
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
    retrieveJsonPaths(t, depth, limit, leafPathsOnly = false, innerPathsOnly = false).drop(1)
  }

  def retrieveJsonPaths(typePath: Uri,
                        depth: Int,
                        limit: Option[Int],
                        leafPathsOnly: Boolean,
                        innerPathsOnly: Boolean,
                        json: JsonTraverser = JsonTraverser(file)(codec)): IndexedSeq[Path] = {
    val subSelectedElements: Seq[JsonTraverser] = navigateToType(typePath, json)
    for (element <- subSelectedElements.headOption.toIndexedSeq; // At the moment, we only retrieve the path from the first found element
         path <- element.collectPaths(path = Nil, leafPathsOnly = leafPathsOnly, innerPathsOnly = innerPathsOnly, depth = depth)) yield {
      Path(path.toList)
    }
  }

  private def navigateToType(typePath: Uri, json: JsonTraverser) = {
    val selectedElements = json.select(basePathParts)
    val subSelectedElements = selectedElements.flatMap(_.select(Path.parse(typePath.uri).operators))
    subSelectedElements
  }

  override def retrieveTypes(limit: Option[Int]): Traversable[(String, Double)] = {
    retrieveJsonPaths("", Int.MaxValue, limit, leafPathsOnly = false, innerPathsOnly = true) map  (p => (p.serialize, 1.0))
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
          val values = for (path <- entityDesc.typedPaths) yield node.evaluate(path.path)
          f(new Entity(uri, values, entityDesc))
        }
      }
    }
  }

  override def peak(entitySchema: EntitySchema, limit: Int): Traversable[Entity] = {
    peakWithMaximumFileSize(file, entitySchema, limit)
  }

  override def extractSchema[T](analyzerFactory: ValueAnalyzerFactory[T],
                                sampleLimit: Option[Int],
                                progress: (Double) => Unit = (_) => {}): ExtractedSchema[T] = {
    val traverser: JsonTraverser = JsonTraverser(file)(codec)
    val typePaths = retrieveJsonPaths(basePath, Int.MaxValue, limit = None, leafPathsOnly = false, innerPathsOnly = true, traverser)
    var typeCounter = 0
    val schemaClasses = for(typePath <- typePaths) yield {
      typeCounter += 1
      val typeTraverser = navigateToType(typePath.serialize, traverser)
      val valuePaths = retrieveJsonPaths(typePath.serialize, depth = 1, limit = None, leafPathsOnly = true, innerPathsOnly = false)
      if(typeCounter % 25 == 0) {
        progress(typeCounter.toDouble / typePaths.size)
      }
      val extractedSchemaPaths = for(valuePath <- valuePaths) yield {
        val analyzer = analyzerFactory.analyzer()
        analyzeValuePath(typeTraverser, valuePath, analyzer, sampleLimit)
        val analyzerResult = analyzer.result
        ExtractedSchemaProperty(valuePath, analyzerResult)
      }
      ExtractedSchemaClass(typePath.serialize, extractedSchemaPaths)
    }
    ExtractedSchema(schemaClasses)
  }

  /** Stops analyzing when the sample limit is reached */
  private def analyzeValuePath[T](traversers: Seq[JsonTraverser],
                                  path: Path,
                                  analyzer: ValueAnalyzer[T],
                                  sampleLimit: Option[Int]): Unit = {
    var analyzedValues = 0
    for(traverser <- traversers if sampleLimit.isEmpty || analyzedValues < sampleLimit.get) {
      val values = traverser.evaluate(path)
      analyzer.update(values)
      analyzedValues += values.size
    }
  }
}
