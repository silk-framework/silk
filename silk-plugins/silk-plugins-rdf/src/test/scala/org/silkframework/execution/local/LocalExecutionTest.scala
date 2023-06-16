package org.silkframework.execution.local

import org.silkframework.workspace.InMemoryWorkspaceTestTrait
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

/**
  * Created on 9/2/16.
  */
class LocalExecutionTest extends AnyFlatSpec with Matchers with InMemoryWorkspaceTestTrait {
  behavior of "Local Execution"

  private val ID: String = "id"

  it should "return the different dataset for the different id" in {
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

  it should "return the same dataset for the same id" in {
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
