package org.silkframework.dataset.sql

import org.silkframework.dataset.Dataset

/**
 * A dataset that provides access to a SQL endpoint.
 */
trait SqlDataset extends Dataset {
  def sqlEndpoint: SqlEndpoint
}
