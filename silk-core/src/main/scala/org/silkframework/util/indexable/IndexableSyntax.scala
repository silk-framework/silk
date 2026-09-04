package org.silkframework.util.indexable

import org.silkframework.util.Indexable

/** Dot-syntax for [[Indexable]]: `c.getAt(idx)`, given evidence for `c`'s type. */
object syntax {

  implicit class IndexableOps[C, A](c: C)(implicit ev: Indexable[C, A]) {
    def getAt(idx: Int): Option[A] = ev.getAt(c, idx)
  }
}
