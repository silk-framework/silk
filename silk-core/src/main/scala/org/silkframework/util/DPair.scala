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

package org.silkframework.util

import org.silkframework.config.Prefixes
import org.silkframework.runtime.resource.{ResourceManager, ResourceLoader}
import org.silkframework.runtime.serialization.{Serialization, XmlFormat}

import scala.language.implicitConversions
import scala.xml.Node

/**
 * Represents a pair of source and target values.
 */
case class DPair[+T](source: T, target: T) {
  def map[U](f: (T) => U) = DPair(f(source), f(target))

  def select(selectSource: Boolean) = if (selectSource) source else target

  def zip[U](pair: DPair[U]) = DPair((source, pair.source), (target, pair.target))

  def reverse = DPair(target, source)
}

/**
 * Provides a number of functions to create pairs and to convert them from/to standard Scala classes.
 */
object DPair {

  /**
   * Creates a DPair from a Scala Pair.
   */
  implicit def fromPair[T](pair: (T, T)): DPair[T] = DPair(pair._1, pair._2)

  /**
   * Converts a DPair to a Scala Pair.
   */
  implicit def toPair[T](p: DPair[T]): (T, T) = (p.source, p.target)

  /**
   * Creates a Pair from a Sequence of 2 values.
   */
  implicit def fromSeq[T](seq: Seq[T]): DPair[T] = DPair(seq(0), seq(1))

  /**
   * Converts a Pair to a Sequence of 2 values.
   */
  implicit def toSeq[T](st: DPair[T]): Seq[T] = Seq(st.source, st.target)

  /**
   * Fills a pair with a given value.
   * The value is evaluated non-strict and thus may yield different values for both evaluations.
   */
  def fill[T](f: => T) = DPair(f, f)

  def generate[T](f: Boolean => T) = DPair(f(true), f(false))

  def empty = DPair[Null](null, null)

  /**
   * Returns the xml serialization format for a pair of values of a specific type T.
   *
   * @param xmlFormat The xml serialization format for type T.
   */
  implicit def dPairFormat[T](implicit xmlFormat: XmlFormat[T]): XmlFormat[DPair[T]] = new PairFormat[T]

  /**
   * XML serialization format.
   */
  private class PairFormat[T](implicit xmlFormat: XmlFormat[T]) extends XmlFormat[DPair[T]] {
    /**
     * Deserialize a value from XML.
     */
    def read(node: Node)(implicit prefixes: Prefixes, resources: ResourceManager) =
      DPair(
        Serialization.fromXml[T]((node \ "Source" \ "_").head),
        Serialization.fromXml[T]((node \ "Target" \ "_").head)
      )

    /**
     * Serialize a value to XML.
     */
    def write(pair: DPair[T])(implicit prefixes: Prefixes): Node =
      <Pair>
        <Source>{Serialization.toXml(pair.source)}</Source>
        <Target>{Serialization.toXml(pair.target)}</Target>
      </Pair>
  }
}
