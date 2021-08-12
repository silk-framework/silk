package org.silkframework.plugins.dataset.json

import com.fasterxml.jackson.core.{JsonFactory, JsonParser, JsonToken}
import org.silkframework.config.Prefixes
import org.silkframework.dataset.DataSource
import org.silkframework.entity.paths.{BackwardOperator, ForwardOperator, PropertyFilter, UntypedPath}
import org.silkframework.entity.{Entity, EntitySchema}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.Resource
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.Identifier
import play.api.libs.json._

import java.net.URLEncoder
import scala.collection.mutable.ArrayBuffer

class JsonReader(resource: Resource) {

  private val reader = initStreamReader()

  private var name = reader.currentName()

  def nextToken(): JsonToken = {
    val token = reader.nextToken()
    if(token == JsonToken.FIELD_NAME) {
      name = reader.getCurrentName
    }
    token
  }

  def currentToken: JsonToken = {
    reader.currentToken()
  }

  def currentName: String = {
    name
  }

  def hasCurrentToken: Boolean = {
    reader.hasCurrentToken
  }

  private def initStreamReader(): JsonParser = {
    val factory = new JsonFactory()
    val parser = factory.createParser(resource.inputStream)
    parser.nextToken()
    parser
    // TODO close reader
  }

  /**
    * Builds a JSON node for a given start element that includes all its children.
    * The parser must be positioned on the start element when calling this method.
    * On return, the parser will be positioned on the element that directly follows the element.
    */
  def buildNode(): JsValue = {
    assert(reader.currentToken == JsonToken.START_OBJECT)
    // Remember label
    var currentLabel: String = null
    // Collect child nodes
    val children = new ArrayBuffer[(String, JsValue)]()
    reader.nextToken()
    while(reader.currentToken != JsonToken.END_OBJECT) {
      reader.currentToken match {
        case JsonToken.FIELD_NAME =>
          currentLabel = reader.currentName
          reader.nextToken()
        case JsonToken.START_OBJECT =>
          children.append(currentLabel -> buildNode())
        case JsonToken.VALUE_STRING =>
          children.append(currentLabel -> JsString(reader.getText))
          reader.nextToken()
        case JsonToken.VALUE_NUMBER_INT |
             JsonToken.VALUE_NUMBER_FLOAT =>
          children.append(currentLabel -> JsNumber(reader.getDecimalValue))
          reader.nextToken()
        case JsonToken.VALUE_TRUE |
             JsonToken.VALUE_FALSE =>
          children.append(currentLabel -> JsBoolean(reader.getBooleanValue))
          reader.nextToken()
        case JsonToken.VALUE_NULL =>
          children.append(currentLabel -> JsNull)
          reader.nextToken()
        case _ =>
          reader.nextToken()
      }
    }

    // Move to the element after the end element.
    reader.nextToken()

    JsObject(children.groupBy(_._1).mapValues(nodes => if(nodes.length == 1) nodes(0)._2 else new JsArray(nodes.map(_._2).toIndexedSeq) ))
  }

}

class JsonStreamReader(taskId: Identifier, resource: Resource, basePath: String, uriPattern: String) {

  private val uriRegex = "\\{([^\\}]+)\\}".r

  private val reader = new JsonReader(resource: Resource)


  /**
    * Retrieves entities from this source which satisfy a specific entity schema.
    *
    * @param entitySchema The entity schema
    * @param limit        Limits the maximum number of retrieved entities
    * @return A Traversable over the entities. The evaluation of the Traversable is non-strict.
    */
  def retrieve(entitySchema: EntitySchema, limit: Option[Int])
              (implicit userContext: UserContext, prefixes: Prefixes): Traversable[Entity] = {
    if(entitySchema.typedPaths.exists(_.operators.exists(_.isInstanceOf[BackwardOperator]))) {
      throw new ValidationException("Backward paths are not supported when streaming JSON. Disable streaming to use backward paths.")
    }

    new Traversable[Entity] {
      override def foreach[U](f: Entity => U): Unit = {
        try {
          val entityPath = UntypedPath.parse(entitySchema.typeUri.uri) ++ entitySchema.subPath
          val entityPathSegments = PathSegments(entityPath)
          var hasNext = goToFirstEntity(reader, entityPath)
          var count = 0
          while(hasNext && limit.forall(count < _)) {
            val node = JsonTraverser(taskId, reader.buildNode())

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
            //if (allowedUris.isEmpty || allowedUris.contains(uri)) {
              // Extract values
              val values = for (path <- entitySchema.typedPaths) yield node.evaluate(path)
              f(Entity(uri, values, entitySchema))
            //}

            hasNext = goToNextEntity(reader, entityPathSegments, entityPathSegments.nrPathSegments - 1)
            count += 1

          }
        } finally {
          // TODO close input stream AND json parser
        }
      }
    }
  }

  /**
    * Moves the parser to the first match of the given path.
    * When returning true, the parser will be positioned on the first start element with the given path.
    * When returning false, no entity with that path has been found.
    * If the path is empty, the base path will be used.
    *
    */
  private def goToFirstEntity(reader: JsonReader, rootPath: UntypedPath): Boolean = {
    val path = if(rootPath.isEmpty) UntypedPath.parse(basePath) else rootPath
    checkObjectPath(path)
    val pathSegments = PathSegments(path)
    goToNextEntity(reader, pathSegments, initialPathSegmentIdx = 0)
  }

  /**
    * Moves the parser to the next element with the provided name on the same hierarchy level.
    *
    * @param entityPathSegments    The entity path leading to the entity.
    * @param initialPathSegmentIdx The path segment the XML stream reader is currently positioned at.
    * @return True, if another element was found. The parser will be positioned on the start element.
    *         False, if the end of the file has been reached.
    */
  private def goToNextEntity(reader: JsonReader,
                             entityPathSegments: PathSegments,
                             initialPathSegmentIdx: Int): Boolean = {
    var pathSegmentIdx = initialPathSegmentIdx
    def currentPathSegment(): entityPathSegments.PathSegment = entityPathSegments.pathSegment(pathSegmentIdx)
    while(reader.hasCurrentToken) {
      reader.currentToken match {
        case JsonToken.START_OBJECT =>
          if(currentPathSegment().matches(reader)) {
            if(pathSegmentIdx == entityPathSegments.nrPathSegments - 1) {
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
    assert(reader.currentToken == JsonToken.START_OBJECT)

    // Move to first child element
    reader.nextToken()
    // Counts the number of elements started
    var elementStartedCount = 1
    while(elementStartedCount > 0) {
      if(reader.currentToken == JsonToken.START_OBJECT) {
        elementStartedCount += 1
      } else if(reader.currentToken == JsonToken.END_OBJECT) {
        elementStartedCount -= 1
      }
      reader.nextToken()
    }
  }

  // TODO can be abstracted
  private def checkObjectPath(path: UntypedPath): Unit = {
    val validationRules = Seq[(String, Boolean)](
      "Only forward operators and property filters on attributes are supported." ->
        path.operators.forall(op => op.isInstanceOf[ForwardOperator] ||
          (op.isInstanceOf[PropertyFilter] && op.asInstanceOf[PropertyFilter].property.uri.startsWith("@")))
      ,
      "No #text path allowed inside object path with streaming mode enabled." ->
        path.operators.filter(_.isInstanceOf[PropertyFilter]).forall(_.asInstanceOf[PropertyFilter].property.uri != "#text")
    )
    for((assertErrorMessage, assertionValue) <- validationRules) {
      assert(assertionValue, assertErrorMessage)
    }
  }

  // TODO can be abstracted
  /** Representation of a path that groups together forward op with related property filters. */
  case class PathSegments(entityPath: UntypedPath) {
    checkObjectPath(entityPath)

    /** The number of path segments. */
    // The root element counts a +1.
    val nrPathSegments: Int = entityPath.operators.count(_.isInstanceOf[ForwardOperator]) + 1

    /** Each path segment consists of a forward path followed by arbitrarily many property filters,
      * except for the first (root) segment, which can only have property filters. */
    private val pathSegments: Array[PathSegment] = {
      val arr = new Array[PathSegment](nrPathSegments)
      var counter = 0
      // Init root segment
      arr(0) = PathSegment(None)
      for(op <- entityPath.operators) {
        op match {
          case fp: ForwardOperator =>
            counter += 1
            arr(counter) = PathSegment(Some(fp))
          case pf: PropertyFilter =>
            arr(counter) = arr(counter).copy(pathFilters = arr(counter).pathFilters :+ pf)
        }
      }
      arr
    }

    def pathSegment(idx: Int): PathSegment = {
      pathSegments(idx)
    }

    /**
      * A path segment groups a forward operator with its related property filters.
      *
      * @param forwardOp   An optional forward operator. This is only None for the root segment.
      * @param pathFilters Path filters that are applied after the corresponding forward operator.
      */
    case class PathSegment(forwardOp: Option[ForwardOperator], pathFilters: Seq[PropertyFilter] = Seq.empty) {
      /** Checks if the path segment matches the current element of the JSON reader. */
      def matches(reader: JsonReader): Boolean = {
        assert(reader.currentToken == JsonToken.START_OBJECT)
        forwardOp forall { op =>
          op.property.uri == reader.currentName
        }
      }
    }
  }

}
