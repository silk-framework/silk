package org.silkframework.workspace.activity.workflow

import play.api.libs.json.{Format, Json}

/** The workflow context a project task is executed in.
  *
  * @param inputTasks The input task IDs of the task in that position of the workflow.
  * @param outputTasks The IDs of the tasks that are connected to the task node in the workflow.
  */
case class WorkflowTaskContext(inputTasks: Option[Seq[WorkflowTaskContextInputTask]],
                               outputTasks: Option[Seq[WorkflowTaskContextOutputTask]])


sealed trait WorkflowTaskContextTask {
  def id: String
}

/** The input task information.
  *
  * @param id The task ID of the input task.
  */
case class WorkflowTaskContextInputTask(id: String) extends WorkflowTaskContextTask

/** The output task information.
  *
  * @param id The task ID of the output task.
  * @param configPort If the task is connected to the config port of the output task. This must be true if inputPort is not defined.
  * @param inputPort The index of the input port. This must be set if configPort == false.
  */
case class WorkflowTaskContextOutputTask(id: String,
                                         configPort: Boolean,
                                         inputPort: Option[Int]) extends WorkflowTaskContextTask {
  assert((configPort || inputPort.isDefined) && !(configPort && inputPort.isDefined) , "Either the config port or a data input port must be specified!")
}

object WorkflowTaskContext {
  implicit val inputTaskFormat: Format[WorkflowTaskContextInputTask] = Json.format[WorkflowTaskContextInputTask]
  implicit val outputTaskFormat: Format[WorkflowTaskContextOutputTask] = Json.format[WorkflowTaskContextOutputTask]
  implicit val workflowTaskContextFormat: Format[WorkflowTaskContext] = Json.format[WorkflowTaskContext]
}