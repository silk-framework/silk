package org.silkframework.util

import org.silkframework.util.indexable.instances.{array, charSequence, seq}
import org.silkframework.util.indexable.syntax._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class IndexableTest extends AnyFlatSpec with Matchers {

  behavior of "Indexable (Seq)"

  it should "resolve a positive in-range index" in {
    Seq(10, 20, 30).getAt(1) shouldBe Some(20)
  }

  it should "resolve a negative in-range index" in {
    Seq(10, 20, 30).getAt(-1) shouldBe Some(30)
  }

  it should "return None for a positive out-of-range index" in {
    Seq(10, 20, 30).getAt(5) shouldBe None
  }

  it should "return None for a negative out-of-range index" in {
    Seq(10, 20, 30).getAt(-4) shouldBe None
  }

  it should "resolve the wrap-to-start boundary" in {
    Seq(10, 20, 30).getAt(-3) shouldBe Some(10)
  }

  it should "return None for Int.MinValue" in {
    Seq(10, 20, 30).getAt(Int.MinValue) shouldBe None
  }

  it should "return None for Int.MaxValue" in {
    Seq(10, 20, 30).getAt(Int.MaxValue) shouldBe None
  }

  it should "return None for an empty container, positive index" in {
    Seq.empty[Int].getAt(0) shouldBe None
  }

  it should "return None for an empty container, negative index" in {
    Seq.empty[Int].getAt(-1) shouldBe None
  }

  behavior of "Indexable (Array)"

  it should "resolve a positive in-range index" in {
    Array(10, 20, 30).getAt(1) shouldBe Some(20)
  }

  it should "resolve a negative in-range index" in {
    Array(10, 20, 30).getAt(-1) shouldBe Some(30)
  }

  it should "return None for a positive out-of-range index" in {
    Array(10, 20, 30).getAt(5) shouldBe None
  }

  it should "return None for a negative out-of-range index" in {
    Array(10, 20, 30).getAt(-4) shouldBe None
  }

  it should "resolve the wrap-to-start boundary" in {
    Array(10, 20, 30).getAt(-3) shouldBe Some(10)
  }

  it should "return None for Int.MinValue" in {
    Array(10, 20, 30).getAt(Int.MinValue) shouldBe None
  }

  it should "return None for Int.MaxValue" in {
    Array(10, 20, 30).getAt(Int.MaxValue) shouldBe None
  }

  it should "return None for an empty container, positive index" in {
    Array.empty[Int].getAt(0) shouldBe None
  }

  it should "return None for an empty container, negative index" in {
    Array.empty[Int].getAt(-1) shouldBe None
  }

  behavior of "Indexable (CharSequence)"

  it should "resolve a positive in-range index on a String" in {
    "abc".getAt(1) shouldBe Some('b')
  }

  it should "resolve a negative in-range index on a String" in {
    "abc".getAt(-1) shouldBe Some('c')
  }

  it should "return None for a positive out-of-range index on a String" in {
    "abc".getAt(5) shouldBe None
  }

  it should "return None for a negative out-of-range index on a String" in {
    "abc".getAt(-4) shouldBe None
  }

  it should "resolve the wrap-to-start boundary on a String" in {
    "abc".getAt(-3) shouldBe Some('a')
  }

  it should "return None for Int.MinValue on a String" in {
    "abc".getAt(Int.MinValue) shouldBe None
  }

  it should "return None for Int.MaxValue on a String" in {
    "abc".getAt(Int.MaxValue) shouldBe None
  }

  it should "return None for an empty String, positive index" in {
    "".getAt(0) shouldBe None
  }

  it should "return None for an empty String, negative index" in {
    "".getAt(-1) shouldBe None
  }

  it should "resolve a positive in-range index on a non-String CharSequence" in {
    new StringBuilder("abc").getAt(1) shouldBe Some('b')
  }

  it should "resolve a negative in-range index on a non-String CharSequence" in {
    new StringBuilder("abc").getAt(-1) shouldBe Some('c')
  }

  it should "return None for an out-of-range index on a non-String CharSequence" in {
    new StringBuilder("abc").getAt(5) shouldBe None
  }

  behavior of "Indexable (RingBuffer)"

  final class RingBuffer[A](capacity: Int) {
    private var elements: Vector[A] = Vector.empty
    def push(a: A): Unit = elements = (elements :+ a).takeRight(capacity)
    def size: Int = elements.size
    def apply(idx: Int): A = elements(idx)
  }

  implicit val ringBufferIndexable: Indexable[RingBuffer[Int], Int] =
    new Indexable[RingBuffer[Int], Int] {
      def length(c: RingBuffer[Int]): Int = c.size
      protected def lookup(c: RingBuffer[Int], idx: Int): Option[Int] = Some(c(idx))
    }

  it should "resolve the oldest surviving element after eviction" in {
    val buffer = new RingBuffer[Int](3)
    Seq(10, 20, 30, 40).foreach(buffer.push)
    buffer.getAt(0) shouldBe Some(20)
  }

  it should "resolve the newest element via a negative index" in {
    val buffer = new RingBuffer[Int](3)
    Seq(10, 20, 30, 40).foreach(buffer.push)
    buffer.getAt(-1) shouldBe Some(40)
  }

  behavior of "Indexable (standalone, no shipped evidence)"

  final case class Sized(size: Int)

  val MagicValue = 42

  def indexableOf(size: Int): Indexable[Sized, Int] =
    new Indexable[Sized, Int] {
      def length(c: Sized): Int = c.size
      protected def lookup(c: Sized, idx: Int): Option[Int] = Some(MagicValue)
    }

  it should "resolve a positive in-range index" in {
    indexableOf(3).getAt(Sized(3), 1) shouldBe Some(MagicValue)
  }

  it should "resolve a negative in-range index" in {
    indexableOf(3).getAt(Sized(3), -1) shouldBe Some(MagicValue)
  }

  it should "return None for a positive out-of-range index" in {
    indexableOf(3).getAt(Sized(3), 5) shouldBe None
  }

  it should "return None for a negative out-of-range index" in {
    indexableOf(3).getAt(Sized(3), -5) shouldBe None
  }

  it should "return None for an empty container" in {
    indexableOf(0).getAt(Sized(0), 0) shouldBe None
  }
}
