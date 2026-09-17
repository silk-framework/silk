package org.silkframework.workspace.activity.workflow

import org.silkframework.config.TaskSpec
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.templating.{TemplateVariable, VariableScope}
import org.silkframework.workspace.{Project, ProjectTask}

import scala.collection.mutable

/**
  * Determines the execution variables that a workflow run needs.
  *
  * Only the execution variables of the executed workflow seed a run. A variable that is referenced at execution
  * time from within the workflow's execution closure therefore has to be defined on the workflow itself, provided
  * when the run is started, or set by a 'Set execution variable' plugin during the run.
  * Execution variables defined on sub-tasks are ignored during a workflow run and are only reported as hints.
  */
object WorkflowExecutionVariables {

  /**
    * The requirement of a single execution variable.
    *
    * @param name         The local name of the variable in the execution scope.
    * @param required     True, if the variable has to be provided when the run is started.
    * @param default      The default defined on the workflow itself, if any.
    * @param definedOn    Sub-tasks that define a default of that name. Those defaults do not apply to the run.
    * @param referencedBy Tasks whose templates reference the variable at execution time.
    * @param setBy        Tasks that set the variable during the run.
    */
  case class ExecutionVariableRequirement(name: String,
                                          required: Boolean,
                                          default: Option[TemplateVariable],
                                          definedOn: Seq[ProjectTask[_ <: TaskSpec]],
                                          referencedBy: Seq[ProjectTask[_ <: TaskSpec]],
                                          setBy: Seq[ProjectTask[_ <: TaskSpec]]) {

    def setDuringExecution: Boolean = setBy.nonEmpty
  }

  /**
    * Analyses the execution closure of a workflow (the workflow itself, its nodes, the tasks those reference and
    * sub-workflows, recursively). Returns all execution variables that are referenced, set or defined on the
    * workflow, sorted by name.
    */
  def apply(workflowTask: ProjectTask[Workflow], project: Project)
           (implicit userContext: UserContext): Seq[ExecutionVariableRequirement] = {
    val subTasks = workflowTask.data.subTasksRecursive(project)
    val closure: Seq[ProjectTask[_ <: TaskSpec]] = workflowTask +: subTasks

    val referencedBy = mutable.LinkedHashMap[String, mutable.Buffer[ProjectTask[_ <: TaskSpec]]]()
    val setBy = mutable.LinkedHashMap[String, mutable.Buffer[ProjectTask[_ <: TaskSpec]]]()
    for (task <- closure) {
      for (variable <- task.data.referencedVariables.distinct if variable.scope == VariableScope.execution) {
        referencedBy.getOrElseUpdate(variable.name, mutable.Buffer()).append(task)
      }
      for (variable <- task.data.modifiedVariables.distinct if variable.scope == VariableScope.execution) {
        setBy.getOrElseUpdate(variable.name, mutable.Buffer()).append(task)
      }
    }

    val defaults = workflowTask.executionVariables.map
    val definedOn = subTasks.flatMap(task => task.executionVariables.variables.map(v => (v.name, task))).groupMap(_._1)(_._2)

    val names = (defaults.keys ++ referencedBy.keys ++ setBy.keys).toSeq.distinct.sorted
    for (name <- names) yield {
      val default = defaults.get(name)
      val setters = setBy.get(name).map(_.toSeq).getOrElse(Seq.empty)
      ExecutionVariableRequirement(
        name = name,
        required = referencedBy.contains(name) && default.isEmpty && setters.isEmpty,
        default = default,
        definedOn = definedOn.getOrElse(name, Seq.empty),
        referencedBy = referencedBy.get(name).map(_.toSeq).getOrElse(Seq.empty),
        setBy = setters
      )
    }
  }
}
