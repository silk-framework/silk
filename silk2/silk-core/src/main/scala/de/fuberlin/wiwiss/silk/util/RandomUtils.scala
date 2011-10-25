package de.fuberlin.wiwiss.silk.util

import util.Random

object RandomUtils {
  def randomElement[T](traversable: Traversable[T]) = {
    val seq = traversable.toIndexedSeq
    seq(Random.nextInt(seq.size))
  }
}