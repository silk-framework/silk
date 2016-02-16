package org.silkframework.plugins.dataset.json

import org.silkframework.entity._
import org.silkframework.runtime.resource.Resource
import org.silkframework.util.Uri
import play.api.libs.json._

import scala.io.Codec

/**
 * Data structure to traverse JSON files.
 * @param parentOpt
 * @param value
 */
case class JsonTraverser(parentOpt: Option[ParentTraverser], value: JsValue) {
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
   * @param path Path prefix to be prepended to all found paths
   * @return Sequence of all found paths
   */
  def collectPaths(path: Seq[PathOperator] = Nil): Seq[Seq[PathOperator]] = {
    value match {
      case obj: JsObject =>
        obj.keys.toSeq.flatMap(key => asNewParent(key, obj.value(key)).collectPaths(path :+ ForwardOperator(key)))
      case array: JsArray if array.value.nonEmpty =>
        keepParent(array.value.head).collectPaths(path)
      case _ => if(path.nonEmpty) Seq(path) else Seq()
    }
  }

  /**
   * Selects all elements in a JSON node matching a path.
   */
  def select(path: Seq[String]): Seq[JsonTraverser] = {
    value match {
      case obj: JsObject if path.nonEmpty =>
        children(path.head).flatMap(value => value.select(path.tail))
      case array: JsArray if array.value.nonEmpty =>
        array.value.flatMap(value => keepParent(value).select(path))
      case _ =>
        Seq(this)
    }
  }

  /**
   * Retrieves all values under a given path.
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
      case (p @ PropertyFilter(prop, _, _)) :: tail =>
        this.value match {
          case obj: JsObject if p.evaluate("\"" + nodeToString(obj.value(prop.uri)) + "\"") =>
            evaluate(tail)
          case array: JsArray if array.value.nonEmpty =>
            array.value.flatMap(v => keepParent(v).evaluate(path))
          case _ =>
            Nil
        }
      case Nil =>
        value match {
          case array: JsArray =>
            array.value.map(nodeToString)
          case _ =>
            Seq(nodeToString(value))
        }
      case l: LanguageFilter =>
        throw new IllegalArgumentException("For JSON, language filters are not applicable.")
    }
  }

  def evaluate(path: Path): Seq[String] = evaluate(path.operators)

  /**
   * Converts a simple json node, such as a number, to a string.
   */
  private def nodeToString(json: JsValue): String = {
    json match {
      case JsBoolean(value) => value.toString
      case JsNumber(value) => value.toString
      case JsString(value) => value.toString
      case _ => json.toString()
    }
  }

  def asNewParent(prop: Uri, value: JsValue) = JsonTraverser(parentOpt = Some(ParentTraverser(this, prop)), value)

  def keepParent(value: JsValue) = JsonTraverser(parentOpt = parentOpt, value)
}

object JsonTraverser {
  def apply(resource: Resource)(implicit codec: Codec): JsonTraverser = {
    JsonTraverser(None, Json.parse(resource.loadAsString))
  }

  def apply(jsValue: JsValue)(implicit codec: Codec): JsonTraverser = {
    JsonTraverser(None, jsValue)
  }
}

case class ParentTraverser(traverser: JsonTraverser, property: Uri)