package org.silkframework.plugins.dataset.json

import com.fasterxml.jackson.core.{JsonFactory, JsonParser}
import org.silkframework.entity.paths.{ForwardOperator, Path, PathOperator, UntypedPath}
import org.silkframework.entity.{Entity, EntitySchema}
import org.silkframework.execution.EntityHolder
import org.silkframework.execution.local.{EmptyEntityTable, GenericEntityTable}
import org.silkframework.runtime.iterator.CloseableIterator
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.resource.Resource
import org.silkframework.util.{Identifier, Uri}

import java.io.{ByteArrayInputStream, InputStream}
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.logging.{Level, Logger}

class JsonSourceInMemory(taskId: Identifier, nodes: () => CloseableIterator[JsonNode], basePath: String, uriPattern: String, navigateIntoArrays: Boolean)
  extends JsonSource(taskId, basePath, uriPattern) {

  private val logger = Logger.getLogger(getClass.getName)

  protected def createParser(): JsonParser = {
    val factory = new JsonFactory()
    nodes().headOption match {
      case Some(node) =>
        factory.createParser(node.toString)
      case None =>
        factory.createParser("[]")
    }
  }

  override def retrieve(entitySchema: EntitySchema, limit: Option[Int] = None)
                       (implicit context: PluginContext): EntityHolder = {
    val entities = nodes().flatMap { node =>
      logger.log(Level.FINE, "Retrieving data from JSON.")
      val jsonTraverser = JsonTraverser.fromNode(underlyingTask.id, node, navigateIntoArrays)
      val selectedElements = jsonTraverser.select(basePathParts)
      val subPath = UntypedPath.parse(entitySchema.typeUri.uri) ++ entitySchema.subPath
      // Check paths
      checkPath(subPath)
      entitySchema.typedPaths.foreach(checkPath)
      // Apply sub path if necessary
      val subPathElements =
        if(subPath.operators.nonEmpty) {
          selectedElements.flatMap(_.select(subPath.operators))
        } else {
          selectedElements
        }
      // Retrieve entities
      retrieveEntities(subPathElements, entitySchema, Set.empty)
    }
    GenericEntityTable(entities, entitySchema, underlyingTask)
  }

  override def retrieveByUri(entitySchema: EntitySchema, entities: Seq[Uri])
                            (implicit context: PluginContext): EntityHolder = {
    if (entities.isEmpty) {
      EmptyEntityTable(underlyingTask)
    } else {
      val retrievedEntities = nodes().flatMap { node =>
        logger.log(Level.FINE, "Retrieving data from JSON.")
        val jsonTraverser = JsonTraverser.fromNode(underlyingTask.id, node, navigateIntoArrays)
        val selectedElements = jsonTraverser.select(basePathParts)
        retrieveEntities(selectedElements, entitySchema, entities.map(_.uri).toSet)
      }
      GenericEntityTable(retrievedEntities, entitySchema, underlyingTask)
    }
  }

  private def retrieveEntities(elements: Seq[JsonTraverser], entityDesc: EntitySchema, allowedUris: Set[String]): Seq[Entity] = {
    for {
      node <- elements
      uri = generateUri(node)
      // Check if this URI should be extracted
      if allowedUris.isEmpty || allowedUris.contains(uri)
    } yield {
      val values = for (path <- entityDesc.typedPaths) yield node.evaluate(path)
      Entity(uri, values, entityDesc)
    }
  }

  /**
    * Generates a URI for a node.
    */
  private def generateUri(node: JsonTraverser): String = {
    if (uriPattern.isEmpty) {
      genericEntityIRI(node.nodeId(node.value))
    } else {
      uriRegex.replaceAllIn(uriPattern, m => {
        val path = UntypedPath.parse(m.group(1)).asStringTypedPath
        val string = node.evaluate(path).mkString
        URLEncoder.encode(string, "UTF8")
      })
    }
  }

  /**
   * Checks that a path is valid, i.e. contains at most one recursive all children operator.
   */
  private def checkPath(path: Path): Unit = {
    val recursiveOperatorCount = path.operators.count {
      case ForwardOperator(prop) if prop.uri == JsonDataset.specialPaths.ALL_CHILDREN_RECURSIVE => true
      case _ => false
    }
    require(recursiveOperatorCount <= 1, s"The ** operator can only occur once in a path. Found: $path")
  }


}

object JsonSourceInMemory {

  def fromString(taskId: Identifier, str: String, basePath: String, uriPattern: String, navigateIntoArrays: Boolean = true): JsonSourceInMemory = {
    new JsonSourceInMemory(taskId, () => new JsonNodeIterator(new ByteArrayInputStream(str.getBytes(StandardCharsets.UTF_8))), basePath, uriPattern, navigateIntoArrays)
  }

  def fromResource(file: Resource, basePath: String, uriPattern: String, navigateIntoArrays: Boolean = true): JsonSourceInMemory = {
    if(file.nonEmpty) {
      new JsonSourceInMemory(Identifier.fromAllowed(file.name), () => new JsonNodeIterator(file.inputStream), basePath, uriPattern, navigateIntoArrays)
    } else {
      new JsonSourceInMemory(Identifier.fromAllowed(file.name), () => CloseableIterator.empty, basePath, uriPattern, navigateIntoArrays)
    }
  }

  /**
   * Reads all top-level nodes from a JSON input stream.
   * If this is a plain JSON file, exactly one node will be returned.
   * If this is a JSON Lines file, a separate node for each line will be returned.
   */
  private class JsonNodeIterator(inputStream: InputStream) extends CloseableIterator[JsonNode]() {
    private val parser = new JsonFactory().createParser(inputStream)
    private val reader = new JsonReader(parser)
    parser.nextToken()

    override def hasNext: Boolean = {
      parser.currentToken() != null
    }

    override def next(): JsonNode = {
      reader.buildNode()
    }

    override def close(): Unit = {
      reader.close()
      inputStream.close()
    }
  }
}
