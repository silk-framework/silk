package org.silkframework.plugins.dataset.json

import com.fasterxml.jackson.core.{JsonFactory, JsonParser}
import org.silkframework.config.Prefixes
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.entity.{Entity, EntitySchema, ValueType}
import org.silkframework.execution.EntityHolder
import org.silkframework.execution.local.{EmptyEntityTable, GenericEntityTable}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.Resource
import org.silkframework.util.{Identifier, Uri}
import play.api.libs.json.{JsArray, JsValue, Json}

import java.net.URLEncoder
import java.util.logging.{Level, Logger}

class JsonSourceInMemory(taskId: Identifier, input: JsValue, basePath: String, uriPattern: String) extends JsonSource(taskId, basePath, uriPattern) {

  private val logger = Logger.getLogger(getClass.getName)

  protected def createParser(): JsonParser = {
    val factory = new JsonFactory()
    factory.createParser(input.toString())
  }

  override def retrieve(entitySchema: EntitySchema, limit: Option[Int] = None)
                       (implicit userContext: UserContext, prefixes: Prefixes): EntityHolder = {
    logger.log(Level.FINE, "Retrieving data from JSON.")
    val jsonTraverser = JsonTraverser(underlyingTask.id, input)
    val selectedElements = jsonTraverser.select(basePathParts)
    val subPath = UntypedPath.parse(entitySchema.typeUri.uri) ++ entitySchema.subPath
    val subPathElements = if(subPath.operators.nonEmpty) {
      selectedElements.flatMap(_.select(subPath.operators))
    } else { selectedElements }
    val entities = new Entities(subPathElements, entitySchema, Set.empty)
    GenericEntityTable(entities, entitySchema, underlyingTask)
  }

  override def retrieveByUri(entitySchema: EntitySchema, entities: Seq[Uri])
                            (implicit userContext: UserContext, prefixes: Prefixes): EntityHolder = {
    if(entities.isEmpty) {
      EmptyEntityTable(underlyingTask)
    } else {
      logger.log(Level.FINE, "Retrieving data from JSON.")
      val jsonTraverser = JsonTraverser(underlyingTask.id, input)
      val selectedElements = jsonTraverser.select(basePathParts)
      val retrievedEntities = new Entities(selectedElements, entitySchema, entities.map(_.uri).toSet)
      GenericEntityTable(retrievedEntities, entitySchema, underlyingTask)
    }
  }

  // TODO remove as this method is only used in tests
  def retrieveJsonPaths(typePath: Uri,
                        depth: Int,
                        limit: Option[Int],
                        leafPathsOnly: Boolean,
                        innerPathsOnly: Boolean,
                        json: JsonTraverser = JsonTraverser(underlyingTask.id, input)): IndexedSeq[(UntypedPath, ValueType)] = {
    val subSelectedElements: Seq[JsonTraverser] = navigateToType(typePath, json)
    for (element <- subSelectedElements.headOption.toIndexedSeq; // At the moment, we only retrieve the path from the first found element
         (path, valueType) <- element.collectPaths(path = Nil, leafPathsOnly = leafPathsOnly, innerPathsOnly = innerPathsOnly, depth = depth)) yield {
      (UntypedPath(path.toList), valueType)
    }
  }

  private def navigateToType(typePath: Uri, json: JsonTraverser) = {
    val selectedElements = json.select(basePathParts)
    val subSelectedElements = selectedElements.flatMap(_.select(UntypedPath.parse(typePath.uri).operators))
    subSelectedElements
  }

  private class Entities(elements: Seq[JsonTraverser], entityDesc: EntitySchema, allowedUris: Set[String]) extends Traversable[Entity] {
    def foreach[U](f: Entity => U) {
      // Enumerate entities
      for ((node, index) <- elements.zipWithIndex) {
        // Generate URI
        val uri =
          if (uriPattern.isEmpty) {
            genericEntityIRI(node.nodeId(node.value))
          } else {
            uriRegex.replaceAllIn(uriPattern, m => {
              val path = UntypedPath.parse(m.group(1)).asStringTypedPath
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

  private def basePathMatches(currentPath: List[String]) = {
    basePathLength == 0 || basePathPartsReversed == currentPath.takeRight(basePathLength)
  }


}

object JsonSourceInMemory {

  def apply(taskId: Identifier, str: String, basePath: String, uriPattern: String): JsonSourceInMemory = {
    new JsonSourceInMemory(taskId, Json.parse(str), basePath, uriPattern)
  }

  def apply(file: Resource, basePath: String, uriPattern: String): JsonSourceInMemory = {
    if(file.nonEmpty) {
      new JsonSourceInMemory(Identifier.fromAllowed(file.name), file.read(Json.parse), basePath, uriPattern)
    } else {
      new JsonSourceInMemory(Identifier.fromAllowed(file.name), JsArray(), basePath, uriPattern)
    }
  }
}
