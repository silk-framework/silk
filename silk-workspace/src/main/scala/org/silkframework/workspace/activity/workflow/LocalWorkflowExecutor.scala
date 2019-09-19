package org.silkframework.workspace.activity.workflow

import java.util.logging.{Level, Logger}

import org.silkframework.config.{PlainTask, Task, TaskSpec}
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.dataset._
import org.silkframework.entity.EntitySchema
import org.silkframework.execution.ExecutorOutput
import org.silkframework.execution.local.{LocalEntities, LocalExecution}
import org.silkframework.plugins.dataset.InternalDataset
import org.silkframework.runtime.activity.{ActivityContext, UserContext}
import org.silkframework.util.Identifier
import org.silkframework.workspace.ProjectTask

import scala.util.control.NonFatal

/**
  * A local workflow executor. This is not thread safe. Usually this should not be executed directly, but instead via [[LocalWorkflowExecutorGeneratingProvenance]].
  *
  * It first builds a dependency graph based on the method from [[Workflow]]. This graph can contain
  * multiple connected components (The dependency graph is double linked). For each end node,
  * i.e. each node with out degree zero in the "following node" direction execution is called
  * recursively.
  *
  * All [[WorkflowDataset]] nodes are only executed once, all other nodes are executed each time
  * they occur in an execution flow.
  *
  * @param useLocalInternalDatasets If true the workflow executor will create internal datasets that are only used
  *                                 for each workflow execution and then discarded.
  *                                 If false the [[InternalDataset]]s are used that can be accessed from the project.
  */
case class LocalWorkflowExecutor(workflowTask: ProjectTask[Workflow],
                                 replaceDataSources: Map[String, Dataset] = Map.empty,
                                 replaceSinks: Map[String, Dataset] = Map.empty,
                                 useLocalInternalDatasets: Boolean = false)
    extends WorkflowExecutor[LocalExecution] {

  private val log = Logger.getLogger(getClass.getName)

  override def initialValue: Option[WorkflowExecutionReport] = Some(WorkflowExecutionReport(workflowTask.taskLabel()))

  override def run(context: ActivityContext[WorkflowExecutionReport])
                  (implicit userContext: UserContext): Unit = {
    cancelled = false

    implicit val workflowRunContext = WorkflowRunContext(
      activityContext = context,
      workflow = currentWorkflow,
      userContext = userContext
    )

    checkVariableDatasets()
    clearOutputDatasets()

    val DAG = workflow.workflowDependencyGraph
    try {
      for (endNode <- DAG.endNodes) {
        executeWorkflowNode(endNode, ExecutorOutput.empty)
      }
      if (workflowRunContext.alreadyExecuted.size != workflow.nodes.size) {
        throw WorkflowException("Not all workflow nodes were executed! Executed " +
            workflowRunContext.alreadyExecuted.size + " of " + workflow.nodes.size + " nodes.")
      }
    } catch {
      case e: WorkflowException =>
        if(!cancelled) {
          throw e // Only rethrow exception if the activity was not cancelled, else the error could be due to the cancellation.
        }
    } finally {
      this.executionContext.executeShutdownHooks()
    }
  }

  private def clearOutputDatasets()(implicit workflowRunContext: WorkflowRunContext): Unit = {
    implicit val userContext: UserContext = workflowRunContext.userContext
    // Clear all internal datasets and input datasets that are configured so
    for (datasetTask <- workflow.outputDatasets(project)(workflowRunContext.userContext)) {
      val usedDatasetTask = resolveDataset(datasetTask, replaceSinks)
      usedDatasetTask.data.entitySink.clear()
    }
  }

  def executeWorkflowNode(node: WorkflowDependencyNode,
                          output: ExecutorOutput)
                         (implicit workflowRunContext: WorkflowRunContext): Option[LocalEntities] = {
    // Execute this node
    if (!cancelled) {
      node.workflowNode match {
        case dataset: WorkflowDataset =>
          executeWorkflowDataset(node, output, dataset)
        case operator: WorkflowOperator =>
          executeWorkflowOperator(node, output, operator)
      }
    } else {
      // Don't execute, workflow has been cancelled
      None
    }
  }

  private def executeWorkflowOperatorInput(input: WorkflowDependencyNode,
                                           output: ExecutorOutput)
                                          (implicit workflowRunContext: WorkflowRunContext): Some[LocalEntities] = {
    executeWorkflowNode(input, output) match {
      case e@Some(entityTable) =>
        e
      case None =>
        throw WorkflowException(s"In workflow ${workflowTask.id.toString} operator node ${input.nodeId} defined an input" +
            s" schema for input $input, but did not receive any result.")
    }
  }

  /** Execute nodes of type [[WorkflowOperator]]. */
  private def executeWorkflowOperator(operatorNode: WorkflowDependencyNode,
                                      executorOutput: ExecutorOutput,
                                      operator: WorkflowOperator)
                                     (implicit workflowRunContext: WorkflowRunContext): Option[LocalEntities] = {
    try {
      project.anyTaskOption(operator.task)(workflowRunContext.userContext) match {
        case Some(operatorTask) =>
          val schemataOpt = operatorTask.data.inputSchemataOpt
          val inputs = operatorNode.inputNodes
          val inputResults = executeWorkflowOperatorInputs(operatorNode, schemataOpt, inputs)

          if (inputResults.exists(_.isEmpty)) {
            throw WorkflowException("At least one input did not return a result for workflow node " + operatorNode.nodeId + "!")
          }
          val result = execute(operatorTask, inputResults.flatten, executorOutput)
          // Throw exception if result was promised, but not returned
          if (operatorTask.data.outputSchemaOpt.isDefined && result.isEmpty) {
            throw WorkflowException(s"In workflow ${workflowTask.id.toString} operator node ${operatorNode.nodeId} defined an output " +
                s"schema, but did not return any result!")
          }
          log.info("Finished execution of " + operator.nodeId)
          workflowRunContext.alreadyExecuted.add(operatorNode.workflowNode)
          updateProgress(operatorNode.nodeId)
          result
        case None =>
          throw WorkflowException("No operator task found with id " + operator.task)
      }
    } catch {
      case ex: WorkflowException =>
        throw ex
      case NonFatal(ex) =>
        log.log(Level.WARNING, "Exception during execution of workflow operator " + operatorNode.workflowNode.nodeId, ex)
        throw WorkflowException("Exception during execution of workflow operator " + operatorNode.workflowNode.nodeId +
          ". Cause: " + ex.getMessage, Some(ex))
    }
  }

  // Execute all inputs of a workflow operator to generate input values for this operator
  private def executeWorkflowOperatorInputs(operatorNode: WorkflowDependencyNode,
                                            schemataOpt: Option[Seq[EntitySchema]],
                                            inputs: Seq[WorkflowDependencyNode])
                                           (implicit workflowRunContext: WorkflowRunContext): Seq[Some[LocalEntities]] = {
    val operatorTask = projectTask(operatorNode.workflowNode.task)
    schemataOpt match {
      case Some(schemata) =>
        val useInputs = checkInputsAgainstSchema(operatorNode, inputs, schemata)
        for ((input, schema) <- useInputs.zip(schemata)) yield {
          executeWorkflowOperatorInput(input, ExecutorOutput(Some(operatorTask), Some(schema)))
        }
      case None =>
        for (input <- inputs) yield {
          executeWorkflowOperatorInput(input, ExecutorOutput(Some(operatorTask), None))
        }
    }
  }

  private def checkInputsAgainstSchema(operatorNode: WorkflowDependencyNode,
                                       inputs: Seq[WorkflowDependencyNode],
                                       schemata: Seq[EntitySchema]): Seq[WorkflowDependencyNode] = {
    if (schemata.size < inputs.size) {
      throw WorkflowException("Number of inputs is larger than the number of input schemata for workflow node "
          + operatorNode.nodeId + ". This cannot be handled!")
    } else if (schemata.nonEmpty && inputs.size < schemata.size && inputs.nonEmpty) {
      // TODO: Temporary hack: Duplicate last input if more schemata are defined. Remove as soon as explicit task ports are implemented.
      val lastInput = inputs.last
      val duplicatedInputs = for (i <- 1 to (schemata.size - inputs.size)) yield lastInput
      inputs ++ duplicatedInputs
      /* TODO: In some cases it should be possible to say "Node does not need an input, but if an input is given, use this schema"
               since this is not possible currently, we ignore this if branch
       */
      //    } else if(schemata.nonEmpty && inputs.isEmpty) {
      //      throw WorkflowException("No inputs found for workflow node " + operatorNode.nodeId + "! There were " + schemata.size + " inputs expected.")
    } else {
      inputs
    }
  }

  /** Execute nodes of type [[WorkflowDataset]].
    * These nodes are treated specially, i.e. they are written to only once. So all preceding nodes don't
    * need to be re-evaluated each time.
    */
  private def executeWorkflowDataset(datasetNode: WorkflowDependencyNode,
                                     output: ExecutorOutput,
                                     dataset: WorkflowDataset)
                                    (implicit workflowRunContext: WorkflowRunContext): Option[LocalEntities] = {
    // Only execute a dataset once, i.e. only execute its inputs once and write them to the dataset.
    if (!workflowRunContext.alreadyExecuted.contains(datasetNode.workflowNode)) {
      // Execute all input nodes and write to this dataset
      datasetNode.precedingNodes foreach { pNode =>
        executeWorkflowNode(pNode, ExecutorOutput(Some(datasetTask(datasetNode.workflowNode.task)), None)) match {
          case Some(entityTable) =>
            writeEntityTableToDataset(dataset, entityTable)
          case None =>
          // Write nothing
        }
      }
      workflowRunContext.alreadyExecuted.add(datasetNode.workflowNode)
      updateProgress(datasetNode.nodeId)
      log.info("Finished writing of node " + datasetNode.nodeId)
    }
    // Read from the dataset
    (output.task, output.requestedSchema) match {
      case (Some(outputTask), Some(entitySchema)) =>
        Some(readFromDataset(dataset, entitySchema, outputTask))
      case _ =>
        None
    }
  }

  private def writeEntityTableToDataset(workflowDataset: WorkflowDataset,
                                        entityTable: LocalEntities)
                                       (implicit workflowRunContext: WorkflowRunContext): Unit = {
    try {
      val resolvedDataset = resolveDataset(datasetTask(workflowDataset.task), replaceSinks)
      execute(resolvedDataset, Seq(entityTable), ExecutorOutput.empty)
    } catch {
      case NonFatal(ex) =>
        throw WorkflowException("Exception occurred while writing to workflow dataset operator " + workflowDataset.nodeId +
            ". Cause: " + ex.getMessage, Some(ex))
    }
  }

  def readFromDataset(workflowDataset: WorkflowDataset,
                      entitySchema: EntitySchema,
                      outputTask: Task[_ <: TaskSpec])
                     (implicit workflowRunContext: WorkflowRunContext): LocalEntities = {
    val resolvedDataset = resolveDataset(datasetTask(workflowDataset.task), replaceDataSources)
    execute(resolvedDataset, Seq.empty, ExecutorOutput(Some(outputTask), Some(entitySchema))) match {
      case Some(entityTable) =>
        entityTable
      case None =>
        throw WorkflowException(s"In workflow ${workflowTask.id.toString} the Dataset node ${workflowDataset.nodeId} did " +
            s"not return any result!")
    }
  }

  /** Update the progress of this activity. */
  private def updateProgress(currentTask: String)
                            (implicit workflowRunContext: WorkflowRunContext): Unit = {
    val progress = workflowRunContext.alreadyExecuted.size.toDouble / workflowNodes.size
    workflowRunContext.activityContext.status.update(s"$currentTask (${workflowRunContext.alreadyExecuted.size} / ${workflowNodes.size})", progress)
  }

  /** NOT USED ANYMORE, only here for documentation reasons, should be deleted after everything in here is supported. */
  def executeOperator(operator: WorkflowNode)
                     (implicit workflowRunContext: WorkflowRunContext): Unit = {
    // Get the error sinks for this operator
    val errorOutputs = operator match {
      case wo: WorkflowOperator => wo.errorOutputs.map(project.anyTask(_)(workflowRunContext.userContext))
      case _ => Seq()
    }
    var errorSinks: Seq[DatasetWriteAccess] = errorOutputSinks(errorOutputs)


    if (errorOutputs.exists(!_.data.isInstanceOf[Dataset])) {
      // TODO: Needs proper graph
      // TODO: How to handle error output in new model?
      errorSinks +:= InternalDataset(null)
    }

    //        val activity = taskExecutor(dataSources, taskData, sinks, errorSinks)
    //        val report = activityContext.child(activity, 0.0).startBlockingAndGetValue()
    //        activityContext.value() = activityContext.value().withReport(operator.id, report)
  }

  private def errorOutputSinks(errorOutputs: Seq[ProjectTask[_ <: TaskSpec]]): Seq[DatasetWriteAccess] = {
    errorOutputs.collect {
      case pt: ProjectTask[_] if pt.data.isInstanceOf[Dataset] =>
        pt.data.asInstanceOf[Dataset]
    }
  }

  /**
    * Returns the dataset that should be used in the workflow. Specifically [[VariableDataset]]
    * and [[InternalDataset]] need to be replaced by the corresponding real dataset.
    *
    * @param datasetTask
    * @param replaceDatasets A map with replacement datasets for [[VariableDataset]] objects.
    * @return
    */
  private def resolveDataset(datasetTask: Task[GenericDatasetSpec],
                             replaceDatasets: Map[String, Dataset]): Task[GenericDatasetSpec] = {
    datasetTask.data.plugin match {
      case _: VariableDataset =>
        replaceDatasets.get(datasetTask.id.toString) match {
          case Some(d) =>
            PlainTask(datasetTask.id, datasetTask.data.copy(plugin = d), metaData = datasetTask.metaData)
          case None =>
            throw new IllegalArgumentException("No input found for variable dataset " + datasetTask.id.toString)
        }

      case _: InternalDataset =>
        val internalDataset = executionContext.createInternalDataset(Some(datasetTask.id.toString))
        PlainTask(datasetTask.id, datasetTask.data.copy(plugin = internalDataset), metaData = datasetTask.metaData)
      case _: Dataset =>
        datasetTask
    }
  }

  override protected val executionContext: LocalExecution = LocalExecution(useLocalInternalDatasets)
}
