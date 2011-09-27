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
