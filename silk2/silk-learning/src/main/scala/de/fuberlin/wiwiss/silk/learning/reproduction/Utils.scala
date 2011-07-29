package de.fuberlin.wiwiss.silk.learning.reproduction

import util.Random
import de.fuberlin.wiwiss.silk.learning.individual.Node

private object Utils {

  def crossoverNodes[T <: Node](n1: List[T], n2: List[T]): List[T] = {
    //Interleave both node lists
    val interleaved = (n1 zip n2).flatMap {
      case (a, b) => if (Random.nextBoolean) a :: b :: Nil else b :: a :: Nil
    }

    //Randomly remove about half of the nodes
    interleaved.filter(_ => Random.nextBoolean)
  }
}