package org.silkframework.dataset.sql

import org.silkframework.dataset.Dataset

trait SqlDataset extends Dataset {
  def sqlEndpoint: SqlEndpoint
}
