package org.silkframework.plugins.dataset.json

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
        obj.value.get(prop.uri).toSeq.map(value => asNewParent(prop, value))
      case array: JsArray if array.value.nonEmpty =>
        array.value.flatMap(v => keepParent(v).children(prop))
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
        asNewParent(key, obj.value(key)).collectPaths(path :+ ForwardOperator(key), leafPathsOnly, innerPathsOnly, depth - 1))
    }

    value match {
      case obj: JsObject =>
        val childPaths = if(depth == 0) Seq() else fetchChildPaths(obj)
        if(leafPathsOnly) {
          childPaths
        } else {
          Seq(path -> UriValueType) ++ childPaths
        }
      case array: JsArray if array.value.nonEmpty =>
        keepParent(array.value.head).collectPaths(path, leafPathsOnly, innerPathsOnly, depth)
      case _ =>
        if (path.nonEmpty && !innerPathsOnly) {
          Seq(path -> StringValueType)
        } else if(innerPathsOnly && path.isEmpty) {
          Seq(path -> UriValueType)
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
        array.value.flatMap(value => keepParent(value).select(path))
      case _: JsArray =>
        Seq()
      case _: JsValue if path.isEmpty =>
        Seq(this)
      case _: JsValue if path.nonEmpty =>
        Seq()
    }
  }

  def select(path: List[PathOperator]): Seq[JsonTraverser] = {
    value match {
      case _: JsObject if path.nonEmpty =>
        selectOnObject(path)
      case array: JsArray if array.value.nonEmpty =>
        val t = array.value.map(value => keepParent(value).select(path))
        t.flatten
      case JsNull =>
        Seq() // JsNull is a JsValue, so it has to be handled before JsValue
      case _: JsValue if path.isEmpty =>
        Seq(this)
      case _ =>
        Seq()
    }
  }

  private def selectOnObject(path: List[PathOperator]) = {
    path.head match {
      case ForwardOperator(prop) =>
        children(prop).flatMap(value => value.select(path.tail))
      case BackwardOperator(_) =>
        parentOpt.toSeq.map(_.traverser)
      case _ =>
        Seq.empty
    }
  }

  /**
    * Retrieves all values under a given path.
    *
    * @param path The path starting from the given json node.
    * @return All found values
    */
  def evaluate(path: Seq[PathOperator]): Seq[String] = {
    path match {
      case ForwardOperator(prop) :: tail =>
        children(prop).flatMap(child => child.evaluate(tail))
      case BackwardOperator(prop) :: tail =>
        parentOpt match {
          case Some(parent) if parent.property == prop =>
            parent.traverser.evaluate(tail)
          case None =>
            Nil
        }
      case (p : PropertyFilter) :: tail =>
        evaluatePropertyFilter(path, p, tail)
      case Nil =>
        nodeToValue(value)
      case l: LanguageFilter =>
        throw new IllegalArgumentException("For JSON, language filters are not applicable.")
    }
  }

  private def evaluatePropertyFilter(path: Seq[PathOperator], filter: PropertyFilter, tail: List[PathOperator]) = {
    this.value match {
      case obj: JsObject if filter.evaluate("\"" + nodeToString(obj.value(filter.property.uri)) + "\"") =>
        evaluate(tail)
      case array: JsArray if array.value.nonEmpty =>
        array.value.flatMap(v => keepParent(v).evaluate(path))
      case _ =>
        Nil
    }
  }

  def nodeToValue(jsValue: JsValue): Seq[String] = {
    jsValue match {
      case array: JsArray =>
        array.value.flatMap(nodeToValue)
      case jsObject: JsObject =>
        Seq(generateUri(parentOpt.map(_.property.uri).getOrElse(""), jsObject))
      case JsNull =>
        Seq()
      case other: JsValue =>
        Seq(nodeToString(other))
    }
  }

  def generateUri(path: String, value: JsObject): String = {
    DataSource.generateEntityUri(path, nodeId(value))
  }

  def nodeId(value: JsValue): String = {
    nodeToString(value).hashCode.toString
  }

  def evaluate(path: UntypedPath): Seq[String] = evaluate(path.operators)

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
    JsonTraverser(taskId, None, Json.parse(resource.loadAsString))
  }

  def apply(taskId: Identifier, jsValue: JsValue): JsonTraverser = {
    JsonTraverser(taskId, None, jsValue)
  }
}

case class ParentTraverser(traverser: JsonTraverser, property: Uri)