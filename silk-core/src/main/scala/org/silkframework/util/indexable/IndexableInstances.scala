package org.silkframework.util.indexable

import org.silkframework.util.Indexable

object instances {

  implicit def seq[A]: Indexable[Seq[A], A] =
    new Indexable[Seq[A], A] {
      def length(c: Seq[A]): Int = c.length
      protected def lookup(c: Seq[A], idx: Int): Option[A] = Some(c(idx))
    }

  implicit def array[A]: Indexable[Array[A], A] =
    new Indexable[Array[A], A] {
      def length(c: Array[A]): Int = c.length
      protected def lookup(c: Array[A], idx: Int): Option[A] = Some(c(idx))
    }

  implicit val charSequence: Indexable[CharSequence, Char] =
    new Indexable[CharSequence, Char] {
      def length(c: CharSequence): Int = c.length
      protected def lookup(c: CharSequence, idx: Int): Option[Char] = Some(c.charAt(idx))
    }
}
