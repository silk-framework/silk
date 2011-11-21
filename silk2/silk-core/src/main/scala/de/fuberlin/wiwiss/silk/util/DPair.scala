/* 
 * Copyright 2009-2011 Freie Universität Berlin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.fuberlin.wiwiss.silk.util

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
  implicit def fromPair[T](pair: (T, T)) = DPair(pair._1, pair._2)

  /**
   * Converts a DPair to a Scala Pair.
   */
  implicit def toPair[T](p: DPair[T]) = Pair(p.source, p.target)

  /**
   * Creates a Pair from a Sequence of 2 values.
   */
  implicit def fromSeq[T](seq: Seq[T]) = DPair(seq(0), seq(1))

  /**
   * Converts a Pair to a Sequence of 2 values.
   */
  implicit def toSeq[T](st: DPair[T]) = Seq(st.source, st.target)

  def fill[T](f: => T) = DPair(f, f)

  def generate[T](f: Boolean => T) = DPair(f(true), f(false))
}
