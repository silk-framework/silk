package org.silkframework.plugins.dataset.json

import java.net.{URLDecoder, URLEncoder}
import java.nio.charset.StandardCharsets

import org.silkframework.dataset.DataSource
import org.silkframework.entity._
import org.silkframework.entity.paths._
import org.silkframework.runtime.resource.Resource
import org.silkframework.util.{Identifier, Uri}
import play.api.libs.json._

/**
  * Data structure to traverse JSON files.
  *
  * @param taskId     - the identifier of the task for which this traverser is executed
  * @param parentOpt - the parent traverser for backward traversal
  * @param value     - the current json object
  */
case class JsonTraverser(taskId: Identifier, parentOpt: Option[ParentTraverser], value: JsValue) {
  def children(prop: Uri): Seq[JsonTraverser] = {
    value match {
      case obj: JsObject =>
        val decodedProp = URLDecoder.decode(prop.uri, StandardCharsets.UTF_8.name)
        obj.value.get(decodedProp).toSeq.map(value => asNewParent(prop, value))
      case array: JsArray if array.value.nonEmpty =>
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

    def fetchChildPaths(obj: JsObject) = {
      obj.keys.toSeq.flatMap(key =>
        asNewParent(key, obj.value(key)).collectPaths(path :+ ForwardOperator(URLEncoder.encode(key, StandardCharsets.UTF_8.name)), leafPathsOnly, innerPathsOnly, depth - 1))
    }

    value match {
      case obj: JsObject =>
        val childPaths = if(depth == 0) Seq() else fetchChildPaths(obj)
        if(leafPathsOnly) {
          childPaths
        } else {
          Seq(path -> ValueType.URI) ++ childPaths
        }
      case array: JsArray if array.value.nonEmpty =>
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
      case _: JsObject if path.nonEmpty =>
        children(path.head).flatMap(value => value.select(path.tail))
      case array: JsArray if array.value.nonEmpty =>
        array.value.flatMap(value => keepParent(value).select(path)).toSeq
      case _: JsArray =>
        Seq()
      case _: JsValue if path.isEmpty =>
        Seq(this)
      case _: JsValue =>
        Seq()
    }
  }

  def select(path: List[PathOperator]): Seq[JsonTraverser] = {
    value match {
      case _: JsObject if path.nonEmpty =>
        selectOnObject(path)
      case array: JsArray if array.value.nonEmpty =>
        val t = array.value.map(value => keepParent(value).select(path))
        t.flatten.toSeq
      case JsNull =>
        Seq() // JsNull is a JsValue, so it has to be handled before JsValue
      case _: JsValue if path.isEmpty =>
        Seq(this)
      case _ =>
        Seq()
    }
  }

  private def selectOnObject(path: List[PathOperator]) = {
    val values = path.head match {
      case ForwardOperator(prop) =>
        children(prop)
      case BackwardOperator(_) =>
        parentOpt.toSeq.map(_.traverser)
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
          case JsonDataset.specialPaths.TEXT =>
            Seq(value.toString())
          case _ =>
            children(prop).flatMap(child => child.evaluate(tail, generateUris))
        }
      case BackwardOperator(_) :: tail =>
        parentOpt match {
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

  private def evaluatePropertyFilter(path: Seq[PathOperator], filter: PropertyFilter, tail: List[PathOperator], generateUris: Boolean): Seq[String] = {
    this.value match {
      case obj: JsObject if filter.evaluate("\"" + nodeToString(obj.value(filter.property.uri)) + "\"") =>
        evaluate(tail, generateUris)
      case array: JsArray if array.value.nonEmpty =>
        array.value.flatMap(v => keepParent(v).evaluate(path, generateUris)).toSeq
      case _ =>
        Nil
    }
  }

  def nodeToValue(jsValue: JsValue, generateUris: Boolean): Seq[String] = {
    jsValue match {
      case array: JsArray =>
        array.value.flatMap(nodeToValue(_, generateUris)).toSeq
      case jsObject: JsObject =>
        Seq(generateUri(jsObject))
      case JsNull =>
        Seq()
      case v: JsValue if !generateUris =>
        Seq(nodeToString(v))
      case v: JsValue =>
        Seq(generateUri(v))
    }
  }

  def generateUri(value: JsValue): String = {
    DataSource.generateEntityUri(taskId, nodeId(value))
  }

  def nodeId(value: JsValue): String = {
    nodeToString(value).hashCode.toString
  }

  def evaluate(path: TypedPath): Seq[String] = evaluate(path.operators, path.valueType == ValueType.URI)

  /**
    * Converts a simple json node, such as a number, to a string.
    */
  private def nodeToString(json: JsValue): String = {
    json match {
      case JsBoolean(v) => v.toString
      case JsNumber(v) => v.toString
      case JsString(v) => v.toString
      case _ => json.toString()
    }
  }

  def asNewParent(prop: Uri, value: JsValue): JsonTraverser = JsonTraverser(taskId, parentOpt = Some(ParentTraverser(this, prop)), value)

  def keepParent(value: JsValue): JsonTraverser = JsonTraverser(taskId, parentOpt = parentOpt, value)
}

object JsonTraverser {
  def apply(taskId: Identifier, resource: Resource): JsonTraverser = {
    JsonTraverser(taskId, None, Json.parse(resource.loadAsString()))
  }

  def apply(taskId: Identifier, jsValue: JsValue): JsonTraverser = {
    JsonTraverser(taskId, None, jsValue)
  }
}

case class ParentTraverser(traverser: JsonTraverser, property: Uri)