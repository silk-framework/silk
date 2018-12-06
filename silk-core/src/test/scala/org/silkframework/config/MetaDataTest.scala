package org.silkframework.config

import org.scalatest.{FlatSpec, MustMatchers}
import MetaData.labelFromId

class MetaDataTest extends FlatSpec with MustMatchers {

  behavior of "MetaData"

  it should "generate user-friendly labels from identifiers" in {
    labelFromId("task") mustBe "task"
    labelFromId("myTask") mustBe "my Task"
    labelFromId("my_Task") mustBe "my Task"
    labelFromId("my_task") mustBe "my task"
    labelFromId("myABC") mustBe "my ABC"
  }

  it should "clean eccenca Corporate Memory identifiers" in {
    labelFromId("datasetresource_1544099996423_customers_csv") mustBe "customers csv"
    labelFromId("transform_datasetresource_1544099996423_customers_csv") mustBe "transform customers csv"
    labelFromId("linking_transform_datasetresource_1544100171360_loans_csv_transform_datasetresource_1544099996423_customers_csv") mustBe
      "linking transform loans csv transform customers csv"
  }

}
