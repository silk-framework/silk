package org.silkframework.util

/**
  * A type class gives a type a new capability from the outside, without modifying the type itself —
  * Seq, Array, and CharSequence all become Indexable without any of them changing. That's only possible
  * if three things stay separate: the trait, which names the capability; evidence, one implicit value
  * per adapted type, proving that type has it; and syntax, which makes the capability callable as
  * ordinary dot-syntax. Extending it to a new type means adding one more evidence value — nothing here
  * changes. Type classes provide ad hoc polymorphism; because they're open, they additionally support
  * retroactive extension — adding this capability to an existing type without modifying it.
  *
  * `Indexable` itself means extended indexability of sequence-like structures: ordinary forward
  * indexing, plus Python-style negative indexing — `-1` the last element, `-2` the second-to-last —
  * wrapping via modulo instead of throwing or requiring a bounds check at the call site.
  *
  * @example {{{
  * // Evidence: give a new type the capability.
  * implicit val ringBufferIndexable: Indexable[RingBuffer[Int], Int] =
  *   new Indexable[RingBuffer[Int], Int] {
  *     def length(c: RingBuffer[Int]): Int = c.size
  *     protected def lookup(c: RingBuffer[Int], idx: Int): Option[Int] = Some(c(idx))
  *   }
  *
  * // Use site, with indexable.syntax imported:
  * ringBuffer.getAt(-1) // last element, or None if empty
  * }}}
  * @tparam C the container type
  * @tparam A the element type
  * @see [[https://rockthejvm.com/articles/why-are-scala-type-classes-useful Why Are Scala Type Classes Useful?]]
  */
trait Indexable[C, A] {

  private implicit class IntOps(private val a: Int) {
    /**
      * The mathematical modulo operation:
      * the result in [0, n) for positive 'n', or (n, 0] for negative 'n'.
      * Always congruent to 'a'.
      */
    def mod(n: Int): Int = ((a % n) + n) % n
  }

  /** The number of elements in `c`. */
  def length(c: C): Int

  /** Only ever called by `getAt` with an index already validated — no bounds check needed here. */
  protected def lookup(c: C, idx: Int): Option[A]

  /** Indexes `c` at `idx`, accepting negative indices Python-style. */
  final def getAt(c: C, idx: Int): Option[A] = {
    val len = length(c)
    if (idx >= -len && idx < len) lookup(c, idx mod len) else None
  }
}
