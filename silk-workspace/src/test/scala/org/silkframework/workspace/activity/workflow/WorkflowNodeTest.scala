package org.silkframework.workspace.activity.workflow

import org.scalatest.{FlatSpec, MustMatchers}

class WorkflowNodeTest extends FlatSpec with MustMatchers {
  behavior of "Workflow Node"

  it should "be able to copy the underlying case objects from the trait" in {
    val wn: WorkflowNode = WorkflowOperator(Seq(), "id", Seq(), Seq(), (0, 0), "", None, Seq.empty)
    wn.copyNode() mustBe wn
    wn.copyNode(task = "newId") mustBe wn.copyNode(task = "newId")
  }
}
