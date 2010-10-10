package de.fuberlin.wiwiss.silk.util

import xml.NodeSeq

class SourceTargetPair[T](sourceValue : T, targetValue : T) extends Pair[T, T](sourceValue, targetValue)
{
  def source = _1
  def target = _2
}
