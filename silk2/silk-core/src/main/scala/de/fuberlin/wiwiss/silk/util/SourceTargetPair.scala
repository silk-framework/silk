package de.fuberlin.wiwiss.silk.util

class SourceTargetPair[T](sourceValue : T, targetValue : T) extends Pair[T, T](sourceValue, targetValue)
{
  def source = _1
  def target = _2

  def map[U](f : (T) => U) = SourceTargetPair(f(sourceValue), f(targetValue))

  def select(selectSource : Boolean) = if(selectSource) source else target
}

object SourceTargetPair
{
  implicit def fromPair[T](pair : (T, T)) = new SourceTargetPair(pair._1, pair._2)

  def fromSeq[T](seq : Seq[T]) = new SourceTargetPair(seq(0), seq(1))

  def apply[T](sourceValue : T, targetValue : T) = new SourceTargetPair(sourceValue, targetValue)

  def unapply[T](pair : SourceTargetPair[T]) : Option[(T, T)] = Some((pair.source, pair.target))
}
