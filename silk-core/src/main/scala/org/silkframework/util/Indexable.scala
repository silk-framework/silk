package org.silkframework.util

trait Indexable[C, A] {

  private implicit class IntOps(private val a: Int) {
    /**
      * The mathematical modulo operation:
      * the result in [0, n) for positive 'n', or (n, 0] for negative 'n'.
      * Always congruent to 'a'.
      */
    def mod(n: Int): Int = ((a % n) + n) % n
  }

  def length(c: C): Int

  protected def lookup(c: C, idx: Int): Option[A]

  final def getAt(c: C, idx: Int): Option[A] = {
    val len = length(c)
    if (idx >= -len && idx < len) lookup(c, idx mod len) else None
  }
}
