package de.fuberlin.wiwiss.silk.util

class SourceTargetPair[T](sourceValue : T, targetValue : T) extends Pair[T, T](sourceValue, targetValue)
{
  def source = _1
  def target = _2
}

object SourceTargetPair
{
  implicit def fromPair[T](pair : (T, T)) = new SourceTargetPair(pair._1, pair._2)

  def apply[T](sourceValue : T, targetValue : T) = new SourceTargetPair(sourceValue, targetValue)

  def unapply[T](pair : SourceTargetPair[T]) = Some((pair.source, pair.target))
}
