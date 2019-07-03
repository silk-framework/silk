/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.silkframework.entity.paths

import java.net.URLEncoder

import org.silkframework.config.Prefixes
import org.silkframework.entity.{StringValueType, UntypedValueType, UriValueType, paths}
import org.silkframework.util.Uri

import scala.util.{Failure, Success, Try}

/**
  * A [[Path]] without an expected type statement. It solely describes the sequence of properties necessary to arrive
  * at the destination object, without any statement about its expected manifestation.
  */
class UntypedPath private[entity](val operators: List[PathOperator]) extends Path  {

  /**
    * Concatenates this path with another path.
    */
  def ++(path: UntypedPath): UntypedPath = UntypedPath(operators ::: path.operators)

  /**
    * Tests if this path equals another path
    */
  override def equals(other: Any): Boolean = {
    other match {
      case p: UntypedPath => normalizedSerialization == p.normalizedSerialization
      case _ => false
    }
  }

  override def hashCode: Int = normalizedSerialization.hashCode

  /** Returns a [[paths.TypedPath]] from this path with string type values. */
  def asStringTypedPath: TypedPath = TypedPath(this.operators, StringValueType, isAttribute = false)

  def asUriTypedPath: TypedPath = TypedPath(this.operators, UriValueType, isAttribute = false)

  /** Returns an untyped ([[org.silkframework.entity.UntypedValueType]]) [[TypedPath]].  */
  def asUntypedValueType: TypedPath = TypedPath(this.operators, UntypedValueType, isAttribute = false)
}

object UntypedPath {

  /** Special path indexes that have a specific meaning for all datasets, where they are used */
  final val IDX_PATH_IDX = -2 // #idx (returns the index of the entity, e.g. in a CSV file the line number)
  final val IDX_PATH_OPERATORS = Seq(ForwardOperator("#idx"))

  def empty: UntypedPath = new UntypedPath(List.empty)

  /**
    * Creates a new path.
    */
  def apply(operators: List[PathOperator]): UntypedPath = {
    new UntypedPath(operators)
  }

  def unapply(path: UntypedPath): Option[List[PathOperator]] = {
    Some(path.operators)
  }

  /**
    * Creates a path consisting of a single property
    */
  def apply(property: String): UntypedPath = apply(Uri(property))

  /**
    * Creates a path consisting of a single property
    */
  def apply(uri: Uri): UntypedPath = {
    if(uri.isValidUri || Uri("http://ex.org/" + uri.uri).isValidUri) {
      apply(ForwardOperator(uri) :: Nil)
    }
    else {
      apply(ForwardOperator(Uri(URLEncoder.encode(uri.uri, "UTF-8"))) :: Nil)
    }
  }

  /**
    * Convenience function for non-strict path parsing. This will always return a Path object (either parsed or fail save wrapped).
    * @param propertyOrPath - the input string (might be serialized path or new (non-encoded) field name)
    * @param prefixes - will be forwarded to parser
    * @return - a Path
    */
  def saveApply(propertyOrPath: String)(implicit prefixes: Prefixes = Prefixes.empty): UntypedPath = parse(propertyOrPath, strict = false)

  /**
    * Parses a path string.
    * @param pathStr - the path string
    * @param strict - Dictates the behaviour when PathParser fails. If false, the erroneous path string is wrapped inside an Uri without syntax test.
    * @return - a Path
    */
  def parse(pathStr: String, strict: Boolean = true)(implicit prefixes: Prefixes = Prefixes.empty): UntypedPath = {
    Try{new PathParser(prefixes).parse(pathStr)} match{
      case Success(p) => p
      case Failure(f) => if(strict) throw f else apply(Uri(pathStr))
    }
  }

  def removePathPrefix(path: UntypedPath, subPath: UntypedPath): UntypedPath= {
    if(path.operators.startsWith(subPath.operators)){
      new UntypedPath(path.operators.drop(subPath.operators.size))
    } else {
      path
    }
  }
}
