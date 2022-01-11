package org.silkframework.util

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object CollectionUtils {

  implicit class ExtendedSeq[T](seq: Seq[T]) {
    /**
      * Implements distinctBy for Sequences as provided by Scala > 2.12
      * Can be removed as soon as we upgrade the Scala version.
      */
    def distinctBy[A](f: T => A): Seq[T] = {
      val buf = new ListBuffer[T]
      val seen = mutable.Set[A]()
      seq foreach { x =>
        val y = f(x)
        if (!seen(y)) {
          buf += x
          seen += y
        }
      }
      buf.toList
    }
  }

}
