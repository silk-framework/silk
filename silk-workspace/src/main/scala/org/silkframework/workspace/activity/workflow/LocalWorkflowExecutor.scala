package org.silkframework.workspace.activity.workflow

import org.silkframework.config.{FixedNumberOfInputs, FlexibleNumberOfInputs, FlexibleSchemaPort, InputPorts, PlainTask, Port, Prefixes, Task, TaskSpec}
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.dataset._
import org.silkframework.entity.EntitySchema
import org.silkframework.execution.local.{ErrorOutputWriter, LocalEntities, LocalExecution}
import org.silkframework.execution.{EntityHolder, ExecutorOutput}
import org.silkframework.plugins.dataset.InternalDataset
import org.silkframework.rule.TransformSpec
import org.silkframework.runtime.activity.{ActivityContext, UserContext}
import org.silkframework.workspace.ProjectTask
import org.silkframework.workspace.activity.transform.TransformTaskUtils._
import org.silkframework.workspace.activity.workflow.ReconfigureTasks.ReconfigurableTask

import java.util.logging.{Level, Logger}
import scala.collection.immutable.Seq
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
                                 useLocalInternalDatasets: Boolean = false,
                                 clearDatasets: Boolean = true)
    extends WorkflowExecutor[LocalExecution] {

  private val log = Logger.getLogger(getClass.getName)

  private implicit val prefixes: Prefixes = workflowTask.project.config.prefixes

  override def initialValue: Option[WorkflowExecutionReport] = Some(WorkflowExecutionReport(workflowTask))

  override def run(context: ActivityContext[WorkflowExecutionReport])
                  (implicit userContext: UserContext): Unit = {
    cancelled = false
    try {
      runWorkflow(context, updateUserContext(userContext))
    } catch {
      case cancelledWorkflowException: StopWorkflowExecutionException =>
        // In case of an cancelled workflow from an operator, the workflow should still be successful, else it would
        context.status.update(cancelledWorkflowException.getMessage, 1)
    }
  }

  private def runWorkflow(implicit context: ActivityContext[WorkflowExecutionReport], userContext: UserContext): Unit = {
    implicit val workflowRunContext: WorkflowRunContext = WorkflowRunContext(
      activityContext = context,
      workflow = currentWorkflow,
      userContext = userContext
    )

    checkReadOnlyDatasets()
    checkVariableDatasets()
    if(clearDatasets) {
      clearOutputDatasets()
    }

    val DAG = workflow.workflowDependencyGraph
    try {
      for (endNode <- DAG.endNodes) {
        executeWorkflowNode(endNode, ExecutorOutput.empty) { _ =>
          // result ignored
        }
      }
      if (workflowRunContext.alreadyExecuted.size != workflow.nodes.size) {
        throw WorkflowExecutionException("Not all workflow nodes were executed! Executed " +
            workflowRunContext.alreadyExecuted.size + " of " + workflow.nodes.size + " nodes.")
      }
    } catch {
      case e: WorkflowExecutionException =>
        if(!cancelled) {
          throw e // Only rethrow exception if the activity was not cancelled, else the error could be due to the cancellation.
        }
    } finally {
      context.value.updateWith(_.asDone())
      this.executionContext.executeShutdownHooks()
    }
  }

  private def clearOutputDatasets()(implicit workflowRunContext: WorkflowRunContext): Unit = {
    implicit val userContext: UserContext = workflowRunContext.userContext
    // Clear all internal datasets and input datasets that are configured so
    // Also clear datasets in sub workflows
    for { currentWorkflow <- workflow +: workflow.subWorkflows(project).map(_.data)
          datasetTask <- currentWorkflow.outputDatasets(project)(workflowRunContext.userContext) } {
      val usedDatasetTask = resolveDataset(datasetTask, replaceSinks)
      usedDatasetTask.data.entitySink.clear()
    }
  }

  // Treat this execution as a dependency execution, not as a data input exececution.
  private def executeAsDependency(node: WorkflowDependencyNode)
                                 (implicit workflowRunContext: WorkflowRunContext): Unit = {
    if (!workflowRunContext.alreadyExecuted.contains(node.workflowNode)) {
      executeWorkflowNode(node, ExecutorOutput.empty) { _ =>
        workflowRunContext.alreadyExecuted.add(node.workflowNode)
      }
    }
  }

  def executeWorkflowNode[T](node: WorkflowDependencyNode,
                             output: ExecutorOutput)
                            (process: Option[LocalEntities] => T)
                            (implicit workflowRunContext: WorkflowRunContext): T = {
    // First execute all execution dependencies of this node
    node.dependencyInputNodes.foreach(executeAsDependency)
    // Execute this node
    if (!cancelled) {
      node.workflowNode match {
        case _: WorkflowDataset =>
          executeWorkflowDataset(node, output)(process)
        case operator: WorkflowOperator =>
          executeWorkflowOperator(node, output, operator)(process)
      }
    } else {
      // Don't execute, workflow has been cancelled
      process(None)
    }
  }

  private def executeWorkflowOperatorInput[T](input: WorkflowDependencyNode,
                                              output: ExecutorOutput,
                                              requestingWorkflowOperator: Task[_ <: TaskSpec])
                                             (process: Option[LocalEntities] => T)
                                             (implicit workflowRunContext: WorkflowRunContext): T = {
    executeWorkflowNode(input, output) {
      case Some(entityTable) =>
        process(Some(entityTable))
      case None if output.requestedSchema.isDefined =>
        val inputTask = task(input)
        throw WorkflowExecutionException(s"Workflow operator '${requestingWorkflowOperator.label(Int.MaxValue)}' defined an input" +
          s" schema for its input '${inputTask.label(Int.MaxValue)}', but did not receive any result from it in workflow '${workflowTask.label(Int.MaxValue)}' .")
      case None =>
        process(None)
    }
  }

  /** Execute nodes of type [[WorkflowOperator]]. */
  private def executeWorkflowOperator[T](operatorNode: WorkflowDependencyNode,
                                         executorOutput: ExecutorOutput,
                                         operator: WorkflowOperator)
                                        (process: Option[LocalEntities] => T)
                                        (implicit workflowRunContext: WorkflowRunContext): T = {
    val operatorTask = task(operatorNode)
    try {
      val inputPorts = operatorTask.data.inputPorts
      val inputs = operatorNode.inputNodes
      executeWorkflowOperatorInputs(operatorNode, inputPorts, inputs) { inputResults =>
        if (executorOutput.requestedSchema.isDefined && inputResults.exists(_.isEmpty)) {
          throw WorkflowExecutionException("At least one input did not return a result for workflow node " + operatorNode.nodeId + "!")
        }
        // Check if this is a nested workflow that has been executed already.
        val isExecutedWorkflow = operatorTask.data.isInstanceOf[Workflow] && workflowRunContext.alreadyExecuted.contains(operatorNode.workflowNode)

        if (!cancelled && !isExecutedWorkflow) {
          executeAndClose("Executing", operatorNode.nodeId, operatorTask, inputResults.flatten, executorOutput) { result =>
            // Throw exception if result was promised, but not returned
            if (operatorTask.data.outputSchemaOpt.isDefined && result.isEmpty) {
              throw WorkflowExecutionException(s"In workflow ${workflowTask.id.toString} operator node ${operatorNode.nodeId} defined an output " +
                s"schema, but did not return any result!")
            }
            log.info("Finished execution of " + operator.nodeId)
            workflowRunContext.alreadyExecuted.add(operatorNode.workflowNode)
            writeErrorOutput(operatorNode, executorOutput)
            process(result)
          }
        } else {
          process(None)
        }
      }
    } catch {
      case ex: WorkflowExecutionException =>
        throw ex
      case ex: StopWorkflowExecutionException =>
        throw ex
      case NonFatal(ex) =>
        log.log(Level.WARNING, s"Execution of '${operatorTask.label()}' (${operatorNode.workflowNode.nodeId}) failed.", ex)
        throw WorkflowExecutionException(s"In '${operatorTask.label()}': " + ex.getMessage, Some(ex))
    }
  }

  // Execute all inputs of a workflow operator to generate input values for this operator
  private def executeWorkflowOperatorInputs[T](operatorNode: WorkflowDependencyNode,
                                               inputPorts: InputPorts,
                                               inputs: Seq[WorkflowDependencyNode])
                                              (process: Seq[Option[LocalEntities]] => T)
                                              (implicit workflowRunContext: WorkflowRunContext): T = {
    val operatorTask = task(operatorNode)

    def executeOnInputs(inputs: Seq[(WorkflowDependencyNode, Port)])(processAll: Seq[Option[LocalEntities]] => T): T = {
      inputs match {
        case Seq() =>
          processAll(Seq.empty)
        case Seq(input) =>
          executeWorkflowOperatorInput(input._1, ExecutorOutput(Some(operatorTask), input._2.schemaOpt), operatorTask) { result =>
            processAll(Seq(result))
          }
        case input +: tail =>
          executeWorkflowOperatorInput(input._1, ExecutorOutput(Some(operatorTask), input._2.schemaOpt), operatorTask) { result =>
            executeOnInputs(tail) { tailInputs =>
              processAll(result +: tailInputs)
            }
          }
      }
    }

    inputPorts match {
      case FixedNumberOfInputs(ports) =>
        /* FIXME: Detect schema mismatch between inputs and requested schemata. Transform input entities to match requested schema automatically
                  and output warning */
        val useInputs = checkInputsAgainstSchema(operatorNode, inputs, ports)
        // Execute "ignorable" inputs as dependencies
        useInputs.drop(ports.size).foreach(executeAsDependency)
        // Only use defined inputs as actual data inputs
        executeOnInputs(useInputs.zip(ports))(process)
      case FlexibleNumberOfInputs() =>
        executeOnInputs(inputs.map(input => (input, FlexibleSchemaPort)))(process)
    }
  }

  private def checkInputsAgainstSchema(operatorNode: WorkflowDependencyNode,
                                       inputs: Seq[WorkflowDependencyNode],
                                       ports: Seq[Port]): Seq[WorkflowDependencyNode] = {
    if (ports.size < inputs.size) {
      inputs
      // Too many inputs are not considered an error anymore.
    } else if (ports.nonEmpty && inputs.size < ports.size && inputs.nonEmpty) {
      // TODO: Temporary hack: Duplicate last input if more schemata are defined. Remove as soon as explicit task ports are implemented.
      val lastInput = inputs.last
      val duplicatedInputs = for (i <- 1 to (ports.size - inputs.size)) yield lastInput
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
  private def executeWorkflowDataset[T](datasetNode: WorkflowDependencyNode,
                                        output: ExecutorOutput)
                                       (process: Option[LocalEntities] => T)
                                       (implicit workflowRunContext: WorkflowRunContext): T = {
    // Only execute a dataset once, i.e. only execute its inputs once and write them to the dataset.
    if (!workflowRunContext.alreadyExecuted.contains(datasetNode.workflowNode)) {
      val task = datasetTask(datasetNode)
      // Execute all input nodes and write to this dataset
      datasetNode.inputNodes foreach { pNode =>
        executeWorkflowNode(pNode, ExecutorOutput(Some(task), None)) {
          case Some(entityTable) =>
            writeEntityTableToDataset(datasetNode, entityTable)
          case None =>
          // Write nothing
        }
      }
      workflowRunContext.alreadyExecuted.add(datasetNode.workflowNode)
      log.info("Finished writing of node " + datasetNode.nodeId)
    }
    // Read from the dataset
    (output.task, output.requestedSchema) match {
      case (Some(outputTask), Some(entitySchema)) =>
        readFromDataset(datasetNode, entitySchema, outputTask) { result =>
          process(Some(result))
        }
      case _ =>
        process(None)
    }
  }

  private def writeEntityTableToDataset(workflowDataset: WorkflowDependencyNode,
                                        entityTable: LocalEntities)
                                       (implicit workflowRunContext: WorkflowRunContext): Unit = {
    val resolvedDataset = resolveDataset(datasetTask(workflowDataset), replaceSinks)
    try {
      executeAndClose("Writing", workflowDataset.nodeId, resolvedDataset, Seq(entityTable), ExecutorOutput.empty) { _ =>
        // ignore result
      }
    } catch {
      case NonFatal(ex) =>
        throw WorkflowExecutionException(s"Exception occurred while writing to dataset '${resolvedDataset.label()}'. Cause: " + ex.getMessage, Some(ex))
    }
  }

  /**
    * Writes the output of a workflow node to a separate error output if configured.
    * Currently only writes error outputs for transformations.
    */
  def writeErrorOutput(operatorNode: WorkflowDependencyNode,
                       executorOutput: ExecutorOutput)
                      (implicit workflowRunContext: WorkflowRunContext): Unit = {
    implicit val userContext: UserContext = workflowRunContext.userContext
    for {
      transformTask <- project.taskOption[TransformSpec](operatorNode.workflowNode.task)
      errorSink <- transformTask.errorEntitySink
    } {
      executeWorkflowOperatorInputs(operatorNode, transformTask.data.inputPorts, operatorNode.inputNodes) { inputResults =>
        executeAndClose("Executing", operatorNode.nodeId, transformTask, inputResults.flatten, executorOutput) { result =>
          for (output <- result) {
            ErrorOutputWriter.write(output, errorSink)
          }
        }
      }
    }
  }

  def readFromDataset[T](workflowDataset: WorkflowDependencyNode,
                         entitySchema: EntitySchema,
                         outputTask: Task[_ <: TaskSpec])
                        (process: LocalEntities => T)
                        (implicit workflowRunContext: WorkflowRunContext): T = {
    val resolvedDataset = resolveDataset(datasetTask(workflowDataset), replaceDataSources)
    executeAndClose("Reading", workflowDataset.nodeId, resolvedDataset, Seq.empty, ExecutorOutput(Some(outputTask), Some(entitySchema))) {
      case Some(entityTable) =>
        process(entityTable)
      case None =>
        throw WorkflowExecutionException(s"In workflow ${workflowTask.id.toString} the Dataset node ${workflowDataset.nodeId} did " +
            s"not return any result!")
    }
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
    replaceDatasets.get(datasetTask.id.toString) match {
      case Some(d) =>
        PlainTask(datasetTask.id, datasetTask.data.copy(plugin = d), metaData = datasetTask.metaData)
      case None =>
        datasetTask.data.plugin match {
          case _: VariableDataset =>
            throw new IllegalArgumentException("No replacement found for variable dataset " + datasetTask.id.toString)
          case _: InternalDataset =>
            val internalDataset = executionContext.createInternalDataset(Some(datasetTask.id.toString))
            PlainTask(datasetTask.id, datasetTask.data.copy(plugin = internalDataset), metaData = datasetTask.metaData)
          case _: Dataset =>
            datasetTask
        }
    }
  }

  override protected val executionContext: LocalExecution = LocalExecution(
    useLocalInternalDatasets,
    replaceDataSources,
    replaceSinks
  )

  override protected def workflowNodeEntities[T](workflowDependencyNode: WorkflowDependencyNode,
                                                 outputTask: Task[_ <: TaskSpec])
                                                (process: Option[EntityHolder] => T)
                                                (implicit workflowRunContext: WorkflowRunContext): T = {
    executeWorkflowNode(workflowDependencyNode, ExecutorOutput(Some(outputTask), Some(outputTask.configSchema)))(process)
  }
}
