package org.silkframework.dataset.sql

import org.silkframework.dataset.Dataset

// TODO implemented by Snowflake, JDBC
trait SqlDataset extends Dataset {
  def sqlEndpoint: SqlEndpoint
}
