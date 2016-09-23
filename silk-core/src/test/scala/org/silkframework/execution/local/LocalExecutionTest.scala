package org.silkframework.execution.local

import org.scalatest.{FlatSpec, ShouldMatchers}

/**
  * Created on 9/2/16.
  */
class LocalExecutionTest extends FlatSpec with ShouldMatchers {
  behavior of "Local Execution"
  System.setProperty("dataset.internal.plugin", "inMemory")

  private val ID: String = "id"

  ignore should "return the different dataset for the different id" in {
    for(useLocalInternalDatasets <- Seq(true, false)) {
      val execution = LocalExecution(useLocalInternalDatasets = useLocalInternalDatasets)
      val noneDs = execution.createInternalDataset(None)
      val idDs = execution.createInternalDataset(Some(ID))
      val otherIdDs = execution.createInternalDataset(Some("otherId"))
      noneDs should not be theSameInstanceAs (idDs)
      idDs should not be theSameInstanceAs (otherIdDs)
      noneDs should not be theSameInstanceAs (otherIdDs)
    }
  }

  ignore should "return the same dataset for the same id" in {
    for(useLocalInternalDatasets <- Seq(true, false)) {
      val execution = LocalExecution(useLocalInternalDatasets = useLocalInternalDatasets)
      execution.createInternalDataset(None) should be theSameInstanceAs  execution.createInternalDataset(None)
      execution.createInternalDataset(Some(ID)) should be theSameInstanceAs execution.createInternalDataset(Some(ID))
    }
  }

  it should "return local internal datasets of useLocalInternalDatasets is set to true" in {
    val execution1 = LocalExecution(useLocalInternalDatasets = true)
    val execution2 = LocalExecution(useLocalInternalDatasets = true)
    execution1.createInternalDataset(None) should not be theSameInstanceAs (execution2.createInternalDataset(None))
    execution1.createInternalDataset(Some(ID)) should not be theSameInstanceAs (execution2.createInternalDataset(Some(ID)))
  }
}
