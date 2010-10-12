package de.fuberlin.wiwiss.silk.util

class SourceTargetPair[T](sourceValue : T, targetValue : T) extends Pair[T, T](sourceValue, targetValue)
{
  def source = _1
  def target = _2
}

object SourceTargetPair
{
  def apply[T](sourceValue : T, targetValue : T) = new SourceTargetPair(sourceValue, targetValue)
}
