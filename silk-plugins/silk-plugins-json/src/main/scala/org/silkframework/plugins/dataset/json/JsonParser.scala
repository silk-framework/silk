package org.silkframework.plugins.dataset.json

import org.silkframework.entity.{ForwardOperator, Path, PathOperator, PropertyFilter}
import org.silkframework.runtime.resource.Resource
import play.api.libs.json._

import scala.io.Codec

/**
 * Implementation of JSON access functions.
 */
object JsonParser {

  /**
   * Loads JSON from a resource.
   */
  def load(resource: Resource)(implicit codec: Codec): JsValue = {
    Json.parse(resource.loadAsString)
  }

  /**
   * Collects all paths from an json node
   * @param json The xml node to search paths in
   * @param path Path prefix to be prepended to all found paths
   * @return Sequence of all found paths
   */
  def collectPaths(json: JsValue, path: Seq[PathOperator] = Nil): Seq[Seq[PathOperator]] = {
    json match {
      case obj: JsObject =>
        obj.keys.toSeq.flatMap(key => collectPaths(obj.value(key), path :+ ForwardOperator(key)))
      case array: JsArray if array.value.nonEmpty =>
        collectPaths(array.value.head, path)
      case _ => if(path.nonEmpty) Seq(path) else Seq()
    }
  }

  /**
   * Selects all elements in a JSON node matching a path.
   */
  def select(json: JsValue, path: Seq[String]): Seq[JsValue] = {
    json match {
      case obj: JsObject if path.nonEmpty =>
        obj.value.get(path.head).toSeq.flatMap(value => select(value, path.tail))
      case array: JsArray if array.value.nonEmpty =>
        array.value.flatMap(value => select(value, path))
      case _ =>
        Seq(json)
    }
  }

  /**
   * Retrieves all values under a given path.
   * @param json The json node from which the values should be retrieved.
   * @param path The path starting from the given json node.
   * @return All found values
   */
  def evaluate(json: JsValue, path: Path): Seq[String] = {
    evaluate(json, path.operators)
  }

  /**
   * Retrieves all values under a given path.
   * @param json The json node from which the values should be retrieved.
   * @param path The path starting from the given json node.
   * @return All found values
   */
  def evaluate(json: JsValue, path: Seq[PathOperator]): Seq[String] = {
    path match {
      case ForwardOperator(prop) :: tail =>
        json match {
          case obj: JsObject =>
            obj.value.get(prop.uri).toSeq.flatMap(value => evaluate(value, tail))
          case array: JsArray if array.value.nonEmpty =>
            array.value.flatMap(value => evaluate(value, path))
          case _ =>
            Nil
        }
      case (p @ PropertyFilter(prop, op, value)) :: tail =>
        json match {
          case obj: JsObject if p.evaluate("\"" + nodeToString(obj.value(prop.uri)) + "\"") =>
            evaluate(obj, tail)
          case array: JsArray if array.value.nonEmpty =>
            array.value.flatMap(value => evaluate(value, path))
          case _ =>
            Nil
        }
      case Nil =>
        json match {
          case array: JsArray =>
            array.value.map(nodeToString)
          case _ =>
            Seq(nodeToString(json))
        }
      case _ =>
        throw new IllegalArgumentException("For JSON only forward and filter operators are supported in paths.")
    }
  }

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

}
