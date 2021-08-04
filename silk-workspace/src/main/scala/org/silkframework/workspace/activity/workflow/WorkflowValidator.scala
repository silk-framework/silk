package org.silkframework.workspace.activity.workflow

import org.silkframework.config.Task
import org.silkframework.runtime.activity.UserContext
import org.silkframework.workspace.exceptions.TaskValidationException
import org.silkframework.workspace.{DefaultTaskValidator, Project}

object WorkflowValidator extends DefaultTaskValidator[Workflow] {

  /**
    * Validates a workflow.
    * This method should be called before adding a workflow to a project.
    * At the moment it only checks the nesting level.
    *
    * @throws TaskValidationException If the workflow validation failed
    */
  override def validate(project: Project, task: Task[Workflow])
                       (implicit userContext: UserContext): Unit = {
    super.validate(project, task)
    checkWorkflowNesting(project, task)
  }

  /**
    * Asserts that no workflow with a nesting level greater than one is created.
    *
    * @throws TaskValidationException If this workflow would create a workflow with nesting level 2
    */
  def checkWorkflowNesting(project: Project, workflow: Task[Workflow])
                          (implicit userContext: UserContext): Unit = {
    val nestedWorkflows = collectNestedWorkflows(project, workflow)

    if(nestedWorkflows.nonEmpty) {
      // Make sure that this workflow does not contain any workflows that itself contain other workflows
      for(nestedWorkflow <- nestedWorkflows) {
        if(collectNestedWorkflows(project, nestedWorkflow).nonEmpty) {
          throw new TaskValidationException(s"Workflow ${workflow.taskLabel()} is not allowed to include ${nestedWorkflow.taskLabel()} " +
            "because that workflow already contains nested workflows.")
        }
      }

      // Make sure that this workflow is not referenced by another workflow
      for (otherWorkflow <- project.tasks[Workflow] if otherWorkflow.id != workflow.id) {
        if(otherWorkflow.data.nodes.exists(_.task == workflow.id)) {
          throw new TaskValidationException(s"Workflow ${workflow.taskLabel()} is not allowed to nest workflows " +
            s"because it's already nested inside ${otherWorkflow.taskLabel()}")
        }
      }
    }
  }

  private def collectNestedWorkflows(project: Project, workflow: Task[Workflow])
                                    (implicit userContext: UserContext): Seq[Task[Workflow]] = {
    for {
      node <- workflow.data.nodes
      nestedWorkflow <- project.taskOption[Workflow](node.task)
    } yield {
      nestedWorkflow
    }
  }

}
