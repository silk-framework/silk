package org.silkframework.plugins.dataset.json

import org.silkframework.dataset.DataSource
import org.silkframework.dataset.DatasetCharacteristics.SpecialPaths
import org.silkframework.entity._
import org.silkframework.entity.paths._
import org.silkframework.runtime.resource.Resource
import org.silkframework.util.{Identifier, Uri}
import play.api.libs.json._

import java.net.{URLDecoder, URLEncoder}
import java.nio.charset.StandardCharsets
import java.util.UUID

/**
  * Data structure to traverse JSON files.
  *
  * @param taskId     - the identifier of the task for which this traverser is executed
  * @param parentOpt - the parent traverser for backward traversal
  * @param value     - the current json object
  */
case class JsonTraverser(taskId: Identifier, parentOpt: Option[ParentTraverser], parentName: Option[String] = None, value: JsonNode, navigateIntoArrays: Boolean) {

  def children(prop: Uri): Seq[JsonTraverser] = {
    value match {
      case obj: JsonObject if prop.uri == JsonDataset.specialPaths.ALL_CHILDREN =>
        for((key, value) <- obj.values.toSeq) yield {
          asNewParent(prop, value, Some(key))
        }
      case obj: JsonObject =>
        val decodedProp = URLDecoder.decode(prop.uri, StandardCharsets.UTF_8.name)
        obj.values.get(decodedProp).toSeq.map(value => asNewParent(prop, value)).flatMap(_.resolveArray())
      case array: JsonArray if array.value.nonEmpty && (navigateIntoArrays || prop.uri == JsonDataset.specialPaths.ARRAY) =>
        array.value.flatMap(v => keepParent(v).children(prop)).toSeq
      case _ =>
        Nil
    }
  }

  /**
    * Collects all paths from an json node. For an array, only the first object is considered.
    *
    * @param path Path prefix to be prepended to all found paths
    * @return Sequence of all found paths with their value type. At the moment only [[StringValueType]] or [[UriValueType]].
    */
  def collectPaths(path: Seq[PathOperator], leafPathsOnly: Boolean, innerPathsOnly: Boolean, depth: Int): Seq[(Seq[PathOperator], ValueType)] = {
    assert(!(leafPathsOnly && innerPathsOnly), "Cannot set leafPathsOnly and innerPathsOnly to true at the same time!")

    def fetchChildPaths(obj: JsonObject) = {
      obj.values.keys.toSeq.flatMap(key =>
        asNewParent(key, obj.values(key)).collectPaths(path :+ ForwardOperator(URLEncoder.encode(key, StandardCharsets.UTF_8.name)), leafPathsOnly, innerPathsOnly, depth - 1))
    }

    value match {
      case obj: JsonObject =>
        val childPaths = if(depth == 0) Seq() else fetchChildPaths(obj)
        if(leafPathsOnly) {
          childPaths
        } else {
          Seq(path -> ValueType.URI) ++ childPaths
        }
      case array: JsonArray if array.value.nonEmpty =>
        keepParent(array.value.head).collectPaths(path, leafPathsOnly, innerPathsOnly, depth)
      case _ =>
        if (path.nonEmpty && !innerPathsOnly) {
          Seq(path -> ValueType.STRING)
        } else if(innerPathsOnly && path.isEmpty) {
          Seq(path -> ValueType.URI)
        } else {
          Seq() // also return root path, since this is a valid type in JSON
        }
    }
  }

  /**
    * Selects all elements in a JSON node matching a path.
    */
  def select(path: Seq[String]): Seq[JsonTraverser] = {
    value match {
      case _: JsonObject | _: JsonArray if path.nonEmpty =>
        children(path.head).flatMap(value => value.select(path.tail))
      case _: JsonNode if path.isEmpty =>
        resolveArray()
      case _: JsonNode =>
        Seq()
    }
  }

  def select(path: List[PathOperator]): Seq[JsonTraverser] = {
    value match {
      case _: JsonObject | _: JsonArray if path.nonEmpty =>
        selectOnObject(path)
      case _: JsonNull =>
        Seq() // JsNull is a JsValue, so it has to be handled before JsValue
      case _: JsonNode if path.isEmpty =>
        Seq(this)
      case _ =>
        Seq()
    }
  }

  private def selectOnObject(path: List[PathOperator]): Seq[JsonTraverser] = {
    val values = path.head match {
      case ForwardOperator(prop) =>
        children(prop)
      case BackwardOperator(_) =>
        navigateBack().toSeq.map(_.traverser)
      case _ =>
        Seq.empty
    }
    values.flatMap(value => value.select(path.tail))
  }

  /**
    * Retrieves all values under a given path.
    *
    * @param path The path starting from the given json node.
    * @return All found values
    */
  def evaluate(path: Seq[PathOperator], generateUris: Boolean): Seq[String] = {
    path match {
      case ForwardOperator(prop) :: tail =>
        prop.uri match {
          case JsonDataset.specialPaths.ID =>
            Seq(nodeId(value))
          case JsonDataset.specialPaths.UUID =>
            Seq(nodeUuid(value))
          case JsonDataset.specialPaths.TEXT =>
            Seq(value.toString())
          case JsonDataset.specialPaths.ARRAY_TEXT =>
            parentOpt.map(_.traverser.value) match {
              case Some(array: JsonArray) =>
                // If the parent is an array, we return the string representation of the whole array
                // As this will be called for each element in the array, we only return it if the current value is the first element of the array
                if (array.value.headOption.contains(value)) {
                  Seq(array.toString())
                } else {
                  Seq.empty
                }
              case _ =>
                // Otherwise, we return the string representation of the current value
                Seq(value.toString())
            }
          case JsonDataset.specialPaths.KEY =>
           parentName.toSeq
          case JsonDataset.specialPaths.ARRAY if !navigateIntoArrays =>
            value match {
              case array: JsonArray if array.value.nonEmpty =>
                array.value.flatMap(v => keepParent(v).evaluate(tail, generateUris)).toSeq
              case _ =>
                Seq()
            }
          case SpecialPaths.LINE.value =>
            Seq(value.position.line.toString)
          case SpecialPaths.COLUMN.value =>
            Seq(value.position.column.toString)
          case _ =>
            children(prop).flatMap(child => child.evaluate(tail, generateUris))
        }
      case BackwardOperator(_) :: tail =>
        navigateBack() match {
          case Some(parent) =>
            parent.traverser.evaluate(tail, generateUris)
          case None =>
            Nil
        }
      case (p : PropertyFilter) :: tail =>
        evaluatePropertyFilter(path, p, tail, generateUris)
      case Nil =>
        nodeToValue(value, generateUris)
      case _: LanguageFilter =>
        throw new IllegalArgumentException("For JSON, language filters are not applicable.")
    }
  }

  /**
   * Navigates back to next property (i.e., non-array) parent.
   * @return
   */
  def navigateBack(): Option[ParentTraverser] = {
    var currentParent = parentOpt
    while (currentParent.isDefined && currentParent.get.property.isEmpty) {
      currentParent = currentParent.get.traverser.parentOpt
    }
    currentParent
  }

  private def evaluatePropertyFilter(path: Seq[PathOperator], filter: PropertyFilter, tail: List[PathOperator], generateUris: Boolean): Seq[String] = {
    this.value match {
      case obj: JsonObject if obj.values.get(filter.property.uri).exists(n => filter.evaluate("\"" + nodeToString(n) + "\"")) =>
        evaluate(tail, generateUris)
      case array: JsonArray if array.value.nonEmpty =>
        array.value.flatMap(v => keepParent(v).evaluate(path, generateUris)).toSeq
      case _ =>
        Nil
    }
  }

  def nodeToValue(jsValue: JsonNode, generateUris: Boolean): Seq[String] = {
    jsValue match {
      case array: JsonArray =>
        array.value.flatMap(nodeToValue(_, generateUris)).toSeq
      case jsObject: JsonObject =>
        Seq(generateUri(jsObject))
      case _: JsonNull =>
        Seq()
      case v: JsonNode if !generateUris =>
        Seq(nodeToString(v))
      case v: JsonNode =>
        Seq(generateUri(v))
    }
  }

  def generateUri(value: JsonNode): String = {
    DataSource.generateEntityUri(taskId, nodeId(value))
  }

  def nodeId(value: JsonNode): String = {
    nodeToString(value).hashCode.toString
  }

  /**
   * Generates a UUID for the given JSON node based on its string representation.
   */
  private def nodeUuid(value: JsonNode): String = {
    UUID.nameUUIDFromBytes(nodeToString(value).getBytes(StandardCharsets.UTF_8)).toString
  }

  def evaluate(path: TypedPath): Seq[String] = evaluate(path.operators, path.valueType == ValueType.URI)

  /**
    * Converts a simple json node, such as a number, to a string.
    */
  private def nodeToString(json: JsonNode): String = {
    json match {
      case JsonBoolean(v, _) => v.toString
      case JsonNumber(v, _) => v.toString
      case JsonString(v, _) => v
      case _ => json.toString
    }
  }

  /**
   * Resolves all nested arrays in a JSON node.
   */
  private def resolveArray(): Seq[JsonTraverser] = {
    value match {
      case array: JsonArray if navigateIntoArrays =>
        array.value.map(keepParent).flatMap(_.resolveArray()).toSeq
      case _ =>
        Seq(this)
    }
  }

  def asNewParent(prop: Uri, value: JsonNode, parentName: Option[String] = None): JsonTraverser = {
    JsonTraverser(taskId, parentOpt = Some(ParentTraverser(this, Some(prop))), parentName = parentName, value, navigateIntoArrays)
  }

  def keepParent(value: JsonNode): JsonTraverser = {
    JsonTraverser(taskId, parentOpt = Some(ParentTraverser(this, None)), parentName = parentName, value, navigateIntoArrays)
  }
}

object JsonTraverser {
  def fromResource(taskId: Identifier, resource: Resource, navigateIntoArrays: Boolean = true): JsonTraverser = {
    JsonTraverser(taskId, None, None, resource.read(JsonNodeSerializer.parse), navigateIntoArrays)
  }

  def fromNode(taskId: Identifier, json: JsonNode, navigateIntoArrays: Boolean, parentName: Option[String] = None): JsonTraverser = {
    JsonTraverser(taskId, None, parentName, json, navigateIntoArrays)
  }
}

case class ParentTraverser(traverser: JsonTraverser, property: Option[Uri])