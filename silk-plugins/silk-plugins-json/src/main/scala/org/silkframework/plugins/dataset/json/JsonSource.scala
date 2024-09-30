package org.silkframework.plugins.dataset.json

import com.fasterxml.jackson.core.{JsonParser, JsonToken}
import org.silkframework.config.{PlainTask, Task}
import org.silkframework.dataset._
import org.silkframework.runtime.resource.Resource
import org.silkframework.util.Identifier

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import scala.collection.mutable
import scala.util.matching.Regex

/**
 * A data source that retrieves all entities from an JSON file.
 *
 * @param input JSON value
 * @param basePath The path to the elements to be read, starting from the root element, e.g., '/Persons/Person'.
 *                 If left empty, all direct children of the root element will be read.
 * @param uriPattern A URI pattern, e.g., http://namespace.org/{ID}, where {path} may contain relative paths to elements
 */
abstract class JsonSource(taskId: Identifier, protected val basePath: String, protected val uriPattern: String) extends DataSource
  with PeakDataSource with HierarchicalSampleValueAnalyzerExtractionSource {

  protected val basePathParts: List[String] = {
    val pureBasePath = basePath.stripPrefix("/").trim
    if (pureBasePath == "") {
      List.empty[String]
    } else {
      pureBasePath.split('/').toList
    }
  }

  override val supportsAsteriskOperator: Boolean = true

  protected val basePathPartsReversed: Seq[String] = basePathParts.reverse

  protected val basePathLength: Int = basePathParts.length

  protected val uriRegex: Regex = "\\{([^\\}]+)\\}".r

  protected def createParser(): JsonParser

  /**
    * Collects all paths from the JSON.
    *
    * @param collectValues A function to collect values of a path.
    * @return all collected paths
    */
  def collectPaths(limit: Int, collectValues: (List[String], String) => Unit = (_, _) => {}): Seq[List[String]] = {
    val jParser = createParser()
    val paths = mutable.HashMap[List[String], Int]()
    paths.put(Nil, 0)
    var idx = 1
    var currentPath = List[String]()

    def stepBack(): Unit = { // Remove last path segment if this is the end of a field value
      if (jParser.getCurrentName != null) {
        currentPath = currentPath.tail
      }
    }

    def handleCurrentToken(token: JsonToken): Unit = {
      token match {
        case JsonToken.START_ARRAY => // Nothing to be done here
        case JsonToken.END_ARRAY =>
          stepBack()
        case JsonToken.FIELD_NAME =>
          currentPath ::= URLEncoder.encode(jParser.getCurrentName, StandardCharsets.UTF_8.name)
          if (basePathMatches(currentPath) && !paths.contains(currentPath.dropRight(basePathLength))) {
            paths.put(if(basePathLength == 0) currentPath else currentPath.dropRight(basePathLength), idx)
            idx += 1
          }
        case JsonToken.START_OBJECT => // Nothing to be done here
        case JsonToken.END_OBJECT =>
          stepBack()
        case jsonValue: JsonToken =>
          jsonValue match { // Collect JSON value
            case JsonToken.VALUE_FALSE | JsonToken.VALUE_TRUE | JsonToken.VALUE_NUMBER_FLOAT | JsonToken.VALUE_NUMBER_INT | JsonToken.VALUE_STRING =>
              if(basePathMatches(currentPath)) {
                collectValues(if(basePathLength == 0) currentPath else currentPath.dropRight(basePathLength), jParser.getValueAsString)
              }
            case _ => // Ignore all other values
          }
          stepBack()
      }
    }

    try {
      while (jParser.nextToken() != null && paths.size < limit) {
        val token = jParser.getCurrentToken
        handleCurrentToken(token)
      }
    } finally {
      jParser.close()
    }
    // Sort paths by first occurrence
    paths.toSeq.sortBy(_._2).map(p => p._1.reverse)
  }

  private def basePathMatches(currentPath: List[String]) = {
    basePathLength == 0 || basePathPartsReversed == currentPath.takeRight(basePathLength)
  }

  /**
    * The dataset task underlying the Datset this source belongs to
    *
    * @return
    */
  override lazy val underlyingTask: Task[DatasetSpec[Dataset]] = PlainTask(taskId, DatasetSpec(EmptyDataset))     //FIXME CMEM 1352 replace with actual task
}

object JsonSource {

  def apply(file: Resource, basePath: String, uriPattern: String): JsonSource = {
    new JsonSourceStreaming(Identifier.fromAllowed(file.name), file, basePath, uriPattern)
  }

}

