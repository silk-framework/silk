package org.silkframework.workspace.activity.workflow

import java.util.logging.Logger

import org.silkframework.dataset.{DataSource, LinkSink, EntitySink, Dataset}
import org.silkframework.plugins.dataset.InternalDataset
import org.silkframework.runtime.activity.{Activity, ActivityContext}
import org.silkframework.workspace.Task
import org.silkframework.workspace.activity.workflow.Workflow.WorkflowOperator

class WorkflowExecutor(task: Task[Workflow]) extends Activity[Unit] {

  val log = Logger.getLogger(getClass.getName)

  @volatile
  private var canceled = false

  override def run(context: ActivityContext[Unit]) = {
    canceled = false
    val project = task.project
    val operators = task.data.operators
    val internalDataset = InternalDataset()
    internalDataset.clear()

    // Clear all internal datasets used as output before writing
    for(datasetId <- operators.flatMap(_.outputs).distinct;
        dataset <- project.taskOption[Dataset](datasetId)
        if dataset.data.plugin.isInstanceOf[InternalDataset]) {
      dataset.data.clear()
    }

    // Preliminary: Just execute the operators from left to right
    for((op, index) <- operators.sortBy(_.position.x).zipWithIndex if !canceled) {
      context.status.update(s"${op.task} (${index + 1} / ${operators.size})", index.toDouble / operators.size)
      executeOperator(op, internalDataset, context)
    }
  }

  override def cancelExecution(): Unit = {
    canceled = true
  }

  def executeOperator(operator: WorkflowOperator, internalDataset: InternalDataset, context: ActivityContext[Unit]) = {
    val project = task.project

    // Get the data sources of this operator
    // Either it reads the data from a dataset or directly from another operator in which case the internal data set is used.
    val inputs = operator.inputs.map(project.anyTask(_).data)
    val dataSources =
      if(inputs.forall(_.isInstanceOf[Dataset]))
        inputs.collect { case ds: Dataset => ds.source }
      else
        Seq(internalDataset.source)

    // Get the sinks for this operator
    val outputs = operator.outputs.map(project.anyTask(_).data)
    var linkSinks = outputs.collect { case ds: Dataset => ds.linkSink }
    var entitySinks = outputs.collect { case ds: Dataset => ds.entitySink }

    if(outputs.exists(!_.isInstanceOf[Dataset])) {
      linkSinks +:= internalDataset.linkSink
      entitySinks +:= internalDataset.entitySink
    }

    // Retrieve the task and its executor
    val taskData = project.anyTask(operator.task).data
    val taskExecutor = project.getExecutor(taskData)
      .getOrElse(throw new Exception("Cannot execute task " + operator.task))

    // Execute the task
    val activity = taskExecutor(dataSources, taskData, linkSinks, entitySinks)
    context.child(activity, 0.0).startBlocking()

    log.info("Finished execution of " + operator.task)
  }
}