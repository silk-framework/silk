package de.fuberlin.wiwiss.silk.util

/**
 * Represents a pair of source and target values.
 */
case class SourceTargetPair[+T](source : T, target : T)
{
  def map[U](f : (T) => U) = SourceTargetPair(f(source), f(target))

  def select(selectSource : Boolean) = if(selectSource) source else target

  def zip[U](pair : SourceTargetPair[U]) = SourceTargetPair((source, pair.source), (target, pair.target))
}

/**
 * Provides a number of functions to create SourceTargetPairs and to convert them from/to standard Scala classes.
 */
object SourceTargetPair
{
  /**
   * Creates a SourceTargetPair from a Pair.
   */
  implicit def fromPair[T](pair : (T, T)) = new SourceTargetPair(pair._1, pair._2)

  /**
   * Converts a SourceTargetPair to a Pair.
   */
  implicit def toPair[T](st : SourceTargetPair[T]) = Pair(st.source, st.target)

  /**
   * Creates a SourceTargetPair from a Sequence of 2 values.
   */
  implicit def fromSeq[T](seq : Seq[T]) = new SourceTargetPair(seq(0), seq(1))

  /**
   * Converts a SourceTargetPair to a Sequence of 2 values.
   */
  implicit def toSeq[T](st : SourceTargetPair[T]) = Seq(st.source, st.target)

  def fill[T](f : => T) = SourceTargetPair(f, f)

  def generate[T](f : Boolean => T) = new SourceTargetPair(f(true), f(false))
}
