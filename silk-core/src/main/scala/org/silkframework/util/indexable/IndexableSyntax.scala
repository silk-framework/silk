package org.silkframework.util.indexable

import org.silkframework.util.Indexable

object syntax {

  implicit class IndexableOps[C, A](c: C)(implicit ev: Indexable[C, A]) {
    def getAt(idx: Int): Option[A] = ev.getAt(c, idx)
  }
}
