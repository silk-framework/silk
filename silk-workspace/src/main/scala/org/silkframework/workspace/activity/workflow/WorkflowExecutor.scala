package org.silkframework.workspace.activity.workflow

import java.util.logging.Logger

import org.silkframework.dataset._
import org.silkframework.execution.{ExecuteTransformResult, ExecutionReport}
import org.silkframework.plugins.dataset.InternalDataset
import org.silkframework.runtime.activity.{Activity, ActivityContext}
import org.silkframework.workspace.{Project, Task}

import scala.collection.immutable.ListMap

class WorkflowExecutor(task: Task[Workflow],
                       replaceDataSources: Map[String, DataSource] = Map.empty,
                       replaceSinks: Map[String, SinkTrait] = Map.empty) extends Activity[WorkflowExecutionReport] {

  val log = Logger.getLogger(getClass.getName)
  private val workflow = task.data
  private val project = task.project

  @volatile
  private var canceled = false

  override def initialValue = Some(WorkflowExecutionReport())

  override def run(context: ActivityContext[WorkflowExecutionReport]) = {
    canceled = false
    val operators = workflow.operators
    val internalDataset = InternalDataset()
    internalDataset.clear()

    clearInternalDatasets(operators)

    checkVariableDatasets()

    // Preliminary: Just execute the operators from left to right
    for ((op, index) <- operators.sortBy(_.position.x).zipWithIndex if !canceled) {
      context.status.update(s"${op.task} (${index + 1} / ${operators.size})", index.toDouble / operators.size)
      executeOperator(op, internalDataset, context)
    }
  }

  private def clearInternalDatasets(operators: Seq[WorkflowOperator]): Unit = {
    // Clear all internal datasets used as output before writing
    for (datasetId <- operators.flatMap(_.outputs).distinct;
         dataset <- project.taskOption[Dataset](datasetId)
         if dataset.data.plugin.isInstanceOf[InternalDataset]) {
      dataset.data.clear()
    }
  }

  // Return error if VariableDataset is used in output and input
  private def checkVariableDatasets(): Unit = {
    val variableDatasets = workflow.variableDatasets(project)
    val notCoveredVariableDatasets = variableDatasets.dataSources.filter(!replaceDataSources.contains(_))
    if(notCoveredVariableDatasets.size > 0) {
      throw new scala.IllegalArgumentException("No replacement for following variable datasets as data sources provided: " + notCoveredVariableDatasets.mkString(", "))
    }
    val notCoveredVariableSinks = variableDatasets.sinks.filter(!replaceSinks.contains(_))
    if(notCoveredVariableSinks.size > 0) {
      throw new scala.IllegalArgumentException("No replacement for following variable datasets as data sinks provided: " + notCoveredVariableSinks.mkString(", "))
    }
  }

  override def cancelExecution(): Unit = {
    canceled = true
  }

  def executeOperator(operator: WorkflowOperator, internalDataset: InternalDataset, context: ActivityContext[WorkflowExecutionReport]): Unit = {
    val project = task.project

    // Get the data sources of this operator
    // Either it reads the data from a dataset or directly from another operator in which case the internal data set is used.
    val inputs = operator.inputs
    val dataSources = inputDatasources(internalDataset, inputs)

    // Get the sinks for this operator
    val outputs = operator.outputs.map(project.anyTask(_).data)
    var sinks: Seq[SinkTrait] = outputSinks(outputs)
    val errorOutputs = operator.errorOutputs.map(project.anyTask(_).data)
    var errorSinks: Seq[SinkTrait] = errorOutputs.collect { case ds: Dataset => ds }

    if (outputs.exists(!_.isInstanceOf[Dataset])) {
      sinks +:= internalDataset
    }
    if (errorOutputs.exists(!_.isInstanceOf[Dataset])) {
      errorSinks +:= internalDataset
    }

    // Retrieve the task and its executor
    val taskData = project.anyTask(operator.task).data
    val taskExecutor = project.getExecutor(taskData)
        .getOrElse(throw new Exception("Cannot execute task " + operator.task))

    // Execute the task
    val activity = taskExecutor(dataSources, taskData, sinks, errorSinks)
    val report = context.child(activity, 0.0).startBlockingAndGetValue()
    context.value() = context.value().withReport(operator.id, report)
    log.info("Finished execution of " + operator.task)
  }

  private def outputSinks(outputs: Seq[Any]): Seq[SinkTrait] = {
    outputs.collect {
      case ds: Dataset if ds.plugin.isInstanceOf[VariableDataset] =>
        replaceSinks.get(ds.id.toString) match {
          case Some(dataSource) => dataSource
          case None =>
            throw new IllegalArgumentException("No output found for variable dataset " + ds.id.toString)
        }
      case ds: Dataset =>
        ds
    }
  }

  private def inputDatasources(internalDataset: InternalDataset, inputIdentifiers: Seq[String]): Seq[DataSource] = {
    val inputs = inputIdentifiers.map(task.project.anyTask(_).data)
    if (inputs.forall(_.isInstanceOf[Dataset])) {
      inputs.collect {
        case ds: Dataset if ds.plugin.isInstanceOf[VariableDataset] =>
          replaceDataSources.get(ds.id.toString) match {
            case Some(dataSource) => dataSource
            case None =>
              throw new IllegalArgumentException("No input found for variable dataset " + ds.id.toString)
          }
        case ds: Dataset =>
          ds.source
      }
    } else {
      Seq(internalDataset.source)
    }
  }
}