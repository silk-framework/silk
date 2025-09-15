package org.silkframework.plugins.dataset.json

import com.fasterxml.jackson.core.{JsonFactoryBuilder, JsonParser, JsonToken, StreamReadFeature}
import org.silkframework.dataset.DataSource
import org.silkframework.entity.paths._
import org.silkframework.entity.{Entity, EntitySchema}
import org.silkframework.execution.EntityHolder
import org.silkframework.execution.local.GenericEntityTable
import org.silkframework.plugins.dataset.json.JsonDataset.specialPaths.{ALL_CHILDREN, ALL_CHILDREN_RECURSIVE}
import org.silkframework.runtime.iterator.BufferingIterator
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.resource.Resource
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.{Identifier, Uri}

import java.net.URLEncoder


class JsonSourceStreaming(taskId: Identifier, resource: Resource, basePath: String, uriPattern: String, navigateIntoArrays: Boolean) extends JsonSource(taskId, basePath, uriPattern) {

  protected def createParser(): JsonParser = {
    val factory = new JsonFactoryBuilder().configure(StreamReadFeature.AUTO_CLOSE_SOURCE, true).build()
    factory.createParser(resource.inputStream)
  }

  override def retrieve(entitySchema: EntitySchema, limit: Option[Int])(implicit context: PluginContext): EntityHolder = {
    val entities = new Entities(entitySchema, limit = limit)
    GenericEntityTable(entities, entitySchema, underlyingTask)
  }

  override def retrieveByUri(entitySchema: EntitySchema, allowedUris: Seq[Uri])
                            (implicit context: PluginContext): EntityHolder = {
    val entities = new Entities(entitySchema, allowedUris = allowedUris.toSet)
    GenericEntityTable(entities, entitySchema, underlyingTask)
  }

  private class Entities(entitySchema: EntitySchema, limit: Option[Int] = None, allowedUris: Set[Uri] = Set.empty) extends BufferingIterator[Entity] {

    private val entityPath = UntypedPath.parse(basePath) ++ UntypedPath.parse(entitySchema.typeUri.uri) ++ entitySchema.subPath

    // Validate paths
    checkPaths(entitySchema.typedPaths, isEntitySelectionPath = false)
    checkPaths(Seq(entityPath), isEntitySelectionPath = true)

    // checkPaths only allows forward operators
    private val entityPathSegments = entityPath.operators.collect{ case op: ForwardOperator => op }.toIndexedSeq

    private var count = 0

    private val reader = new JsonReader(createParser())

    // The next entity
    private var nextEntity: Option[Entity] = None

    // True, if there are unread entities
    private var hasMoreEntities: Boolean = true

    /**
      * Reads to the next entity.
      * Sets `nextEntity` and `hasMoreEntities`.
      */
    override def retrieveNext(): Option[Entity] = {
      if(count == 0) {
        // Read until first entity
        hasMoreEntities = goToFirstEntity(reader, entityPathSegments)
      }

      nextEntity = None
      while (nextEntity.isEmpty && hasMoreEntities && limit.forall(count < _)) {
        val parentName = reader.currentName
        val node = JsonTraverser.fromNode(taskId, reader.buildNode(), navigateIntoArrays, parentName)

        // Generate URI
        val uri =
          if (uriPattern.isEmpty) {
            DataSource.generateEntityUri(taskId, node.nodeId(node.value))

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
          val values = for (path <- entitySchema.typedPaths) yield node.evaluate(path)
          nextEntity = Some(Entity(uri, values, entitySchema))
        }

        hasMoreEntities = goToNextEntity(reader, entityPathSegments, entityPathSegments.size - 1)
        count += 1
      }
      nextEntity
    }

    override def close(): Unit = {
      reader.close()
    }
  }

  /**
    * Moves the parser to the first match of the given path.
    * When returning true, the parser will be positioned on the first start element with the given path.
    * When returning false, no entity with that path has been found.
    * If the path is empty, the base path will be used.
    *
    */
  private def goToFirstEntity(reader: JsonReader, entityPathSegments: IndexedSeq[ForwardOperator]): Boolean = {
    reader.nextToken()
    goToNextEntity(reader, entityPathSegments, initialPathSegmentIdx = -1)
  }

  /**
    * Moves the parser to the next element with the provided name on the same hierarchy level.
    *
    * @param entityPathSegments    The entity path leading to the entity.
    * @param initialPathSegmentIdx The path segment the JSON stream reader is currently positioned at.
    * @return True, if another element was found. The parser will be positioned on the start element.
    *         False, if the end of the file has been reached.
    */
  private def goToNextEntity(reader: JsonReader,
                             entityPathSegments: IndexedSeq[ForwardOperator],
                             initialPathSegmentIdx: Int): Boolean = {
    var pathSegmentIdx = initialPathSegmentIdx
    def currentPathSegment(): Option[ForwardOperator] = if(pathSegmentIdx >= 0) Some(entityPathSegments(pathSegmentIdx)) else None
    while(reader.hasCurrentToken) {
      reader.currentToken match {
        case JsonToken.START_OBJECT |
             JsonToken.VALUE_STRING |
             JsonToken.VALUE_NUMBER_INT |
             JsonToken.VALUE_NUMBER_FLOAT |
             JsonToken.VALUE_FALSE |
             JsonToken.VALUE_TRUE =>

          // Handle recursive ALL_CHILDREN_RECURSIVE specially
          val curSegOpt = currentPathSegment()
          if(curSegOpt.exists(_.property.uri == ALL_CHILDREN_RECURSIVE)) {
            // If recursive is the last segment, any descendant matches
            if(pathSegmentIdx == entityPathSegments.size - 1) {
              return true
            } else {
              // Attempt to match the segment after the recursive operator at any depth
              val nextSeg = entityPathSegments(pathSegmentIdx + 1)

              // If the next segment is also recursive, just advance and continue
              if(nextSeg.property.uri == ALL_CHILDREN_RECURSIVE) {
                pathSegmentIdx += 1
                reader.nextToken()
              } else {
                // If the next segment matches the current token, treat it as matched
                val matchesNow = nextSeg.property.uri == ALL_CHILDREN || nextSeg.property.uri == reader.currentNameEncoded
                if(matchesNow) {
                  // Move to the matched segment
                  pathSegmentIdx += 1
                  if(pathSegmentIdx == entityPathSegments.size - 1) {
                    return true
                  } else {
                    // Advance to next segment after the matched one and continue traversal
                    pathSegmentIdx += 1
                    reader.nextToken()
                  }
                } else {
                  // Scan forward to find a matching token for nextSeg at any depth
                  var found = false
                  while(reader.hasCurrentToken && !found) {
                    reader.currentToken match {
                      case JsonToken.START_OBJECT |
                           JsonToken.VALUE_STRING |
                           JsonToken.VALUE_NUMBER_INT |
                           JsonToken.VALUE_NUMBER_FLOAT |
                           JsonToken.VALUE_FALSE |
                           JsonToken.VALUE_TRUE =>
                        if(nextSeg.property.uri == ALL_CHILDREN || nextSeg.property.uri == reader.currentNameEncoded) {
                          found = true
                          pathSegmentIdx += 1
                          if(pathSegmentIdx == entityPathSegments.size - 1) {
                            return true
                          } else {
                            pathSegmentIdx += 1
                            reader.nextToken()
                          }
                        } else {
                          reader.nextToken()
                        }
                      case _ =>
                        reader.nextToken()
                    }
                  }
                  if(!found) {
                    return false
                  }
                }
              }
            }
          } else if(curSegOpt.forall(segment => segment.property.uri == ALL_CHILDREN || segment.property.uri == reader.currentNameEncoded)) {
            if(pathSegmentIdx == entityPathSegments.size - 1) {
              // All path segments were matching, found element.
              return true
            } else {
              // Last path segment was matching, check next one
              pathSegmentIdx += 1
              reader.nextToken()
            }
          } else {
            // Path element was not matching, check next sibling
            skipElement(reader)
            // skipElement already calls reader.next() at the end, so this is not needed here.
          }
        case JsonToken.END_OBJECT =>
          pathSegmentIdx -= 1
          reader.nextToken()
        case _ =>
          reader.nextToken()
      }
    }
    // Document end, no further entity can be found.
    false
  }

  /**
    * Skips an element.
    * The parser must be positioned on the start element when calling this method.
    * On return, the parser will be positioned on the element that directly follows the element.
    */
  private def skipElement(reader: JsonReader): Unit = {
    assert(reader.currentToken != JsonToken.START_ARRAY)
    if(reader.currentToken == JsonToken.START_OBJECT) {
      // Move to first element
      reader.nextToken()
      // Counts the number of elements started
      var elementStartedCount = 1
      while (elementStartedCount > 0) {
        if (reader.currentToken == JsonToken.START_OBJECT) {
          elementStartedCount += 1
        } else if (reader.currentToken == JsonToken.END_OBJECT) {
          elementStartedCount -= 1
        }
        reader.nextToken()
      }
    } else {
      reader.nextToken()
    }
  }

  private def checkPaths(paths: Iterable[Path], isEntitySelectionPath: Boolean): Unit = {
    for(path <- paths; op <- path.operators) {
      op match {
        case _: BackwardOperator =>
          throw new ValidationException("Backward paths are not supported when streaming JSON. Disable streaming to use backward paths.")
        case _: LanguageFilter =>
          throw new ValidationException("Language filters are not supported for JSON.")
        case _: PropertyFilter if isEntitySelectionPath =>
          throw new ValidationException("Property filters are not supported for selecting JSON entities.")
        case _ =>
      }
    }
  }
}
