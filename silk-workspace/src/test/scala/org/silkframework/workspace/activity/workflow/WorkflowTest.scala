package org.silkframework.workspace.activity.workflow

import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}
import org.silkframework.dataset.Dataset
import org.silkframework.util.Identifier
import org.silkframework.workspace.{ProjectTask, Project}

/**
  * Created on 7/21/16.
  */
class WorkflowTest extends FlatSpec with Matchers with MockitoSugar {
  behavior of "Workflow"

  it should "support sorting its workflow operators topologically" in {
    val project = mock[Project]
    val workflow = Workflow(
      Identifier("workflow"),
      operators = Seq(
        operator(task = "transform1", inputs = Seq("dsA1"), outputs = Seq("dsB1")),
        operator(task = "transform2", inputs = Seq("dsA2"), outputs = Seq("dsB1")),
        operator(task = "linking", inputs = Seq("dsB1", "dsB1"), outputs = Seq("links")),
        operator(task = "generateOutput", inputs = Seq("links"), outputs = Seq("output"))
      ),
      datasets = Seq(
        dataset("dsA1"),
        dataset("dsA2"),
        dataset("dsB1"),
        dataset("links"),
        dataset("output")
      ))
    for(dataset <- workflow.datasets) {
      val id = Identifier(dataset.task)
      val datasetTask = mock[ProjectTask[Dataset]]
      when(datasetTask.id).thenReturn(id)
      when(project.taskOption[Dataset](id)).thenReturn(Some(datasetTask))
    }
    val sortedOperators = workflow.topologicalSortedOperators(project).map(_.task)
    sortedOperators shouldBe Seq("transform1", "transform2", "linking", "generateOutput")
  }

  def operator(task: String, inputs: Seq[String], outputs: Seq[String]): WorkflowOperator = {
    WorkflowOperator(inputs = inputs, task = task, outputs = outputs, Seq(), (0,0), task)
  }

  def dataset(task: String): WorkflowDataset = {
    WorkflowDataset(Seq(), task, Seq(), (0, 0), task)
  }
}
