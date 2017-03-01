package org.silkframework.dataset

import org.silkframework.entity.Path

/**
  * A dataset extension that allows to retrieve example values for a specific path quickly.
  */
trait PeakDataSource { DataSource =>
  def peak(path: Path, limit: Int): Seq[String]
}
