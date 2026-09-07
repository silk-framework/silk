package org.silkframework.workspace.changes

import org.silkframework.config.{CustomTask, PlainTask, Task, TaskSpec}
import org.silkframework.dataset.DatasetSpec
import org.silkframework.rule.{LinkSpec, TransformSpec}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.types.ResourceOption
import org.silkframework.runtime.plugin.{AnyPlugin, PluginContext, PluginObjectParameterTypeTrait, PluginParameter, StringParameterType}
import org.silkframework.runtime.resource.Resource
import org.silkframework.runtime.templating.{TemplateVariable, TemplateVariables}
import org.silkframework.util.Identifier
import org.silkframework.workspace.activity.workflow.Workflow
import org.silkframework.workspace.{Project, ProjectTask}

/** Adds a task to the project. Recorded whenever a task is added. */
case class AddTask(task: PlainTask[TaskSpec]) extends Change with NamesTask {

  override def taskId: Identifier = task.id

  override def taskLabel: Option[String] = Change.capturedName(task)

  override def summary: String = s"Added ${TaskChanges.kind(task.data)} '${task.labelOrId}'"

  override def inverse: Option[RemoveTask] = Some(RemoveTask(task))

  override def applyTo(project: Project)(implicit userContext: UserContext): Unit = {
    if(project.anyTaskOption(task.id).isDefined) {
      throw ChangeConflictException(s"Task '${task.labelOrId}' already exists in project '${project.id}'.")
    }
    project.restoreTask(task)
  }

  // The task parameters may be sensitive, so they are never printed.
  override def toString: String = s"AddTask(${task.id})"
}

/** Removes a task from the project. Holds the removed task, so the removal can be reverted. */
case class RemoveTask(task: PlainTask[TaskSpec]) extends Change with NamesTask {

  override def taskId: Identifier = task.id

  override def taskLabel: Option[String] = Change.capturedName(task)

  override def summary: String = s"Removed ${TaskChanges.kind(task.data)} '${task.labelOrId}'"

  override def inverse: Option[AddTask] = Some(AddTask(task))

  override def applyTo(project: Project)(implicit userContext: UserContext): Unit = {
    TaskChanges.expectState(project, task)
    project.removeAnyTask(task.id, removeDependentTasks = false)
  }

  override def toString: String = s"RemoveTask(${task.id})"
}

/**
  * Replaces a whole task. Recorded when a task is updated without a typed change, e.g. by a whole-task save.
  * Reverting requires the task to be unchanged since, as the whole task is restored.
  */
case class ReplaceTask(before: PlainTask[TaskSpec], after: PlainTask[TaskSpec]) extends Change with NamesTask {

  override def taskId: Identifier = before.id

  // Names the task as the update left it; a rename mentions the previous name.
  override def taskLabel: Option[String] = Change.capturedName(after)

  override def summary: String = {
    val renamed = if(after.labelOrId != before.labelOrId) s", renamed from '${before.labelOrId}'" else ""
    s"Updated ${TaskChanges.kind(after.data)} '${after.labelOrId}'$renamed"
  }

  override def details: Seq[ChangeDetail] = TaskChanges.diff(before, after)

  override def inverse: Option[ReplaceTask] = Some(ReplaceTask(after, before))

  override def applyTo(project: Project)(implicit userContext: UserContext): Unit = {
    val task = TaskChanges.expectState(project, before)
    // Timestamps and users are dropped, so the update is stamped as a new modification.
    task.update(after.data, Some(after.metaData.withoutUserData), Some(after.executionVariables))
  }

  override def toString: String = s"ReplaceTask($taskId)"
}

object TaskChanges {

  /** Names the kind of task for display, e.g. "Text dataset", "transform" or "workflow". */
  def kind(spec: TaskSpec): String = spec match {
    case dataset: DatasetSpec[_] =>
      val label = dataset.plugin.pluginSpec.label
      if(label.toLowerCase.endsWith("dataset")) label else s"$label dataset"
    case _: TransformSpec => "transform"
    case _: LinkSpec => "linking task"
    case _: Workflow => "workflow"
    case custom: CustomTask => s"${custom.pluginSpec.label} task"
    case _ => "task"
  }

  /**
    * What an update changed: parameters by their labels with the previous and the new value, an object parameter
    * such as the mapping rules by name only, a dataset's own settings, a workflow's nodes and edges, the metadata
    * and the execution variables. Passwords and sensitive variables are never printed. Empty if nothing is detected,
    * e.g. for a plugin without value equality.
    */
  def diff(before: Task[TaskSpec], after: Task[TaskSpec]): Seq[ChangeDetail] = {
    implicit val context: PluginContext = PluginContext.empty
    val data = (before.data, after.data) match {
      case (b: Workflow, a: Workflow) =>
        WorkflowDiff(b, a)
      case (b: DatasetSpec[_], a: DatasetSpec[_]) =>
        plugins(b.plugin, a.plugin) ++
          changed("URI attribute", b.uriAttribute.map(_.uri).getOrElse(""), a.uriAttribute.map(_.uri).getOrElse("")) ++
          changed("Read-only", b.readOnly.toString, a.readOnly.toString)
      case (b: AnyPlugin, a: AnyPlugin) =>
        plugins(b, a)
      case _ =>
        Seq.empty
    }
    val metaData =
      Seq("description" -> (before.metaData.description != after.metaData.description),
          "tags" -> (before.metaData.tags != after.metaData.tags)).collect { case (field, true) => ChangeDetail(s"$field changed") }
    data ++ metaData ++ variables(before.executionVariables, after.executionVariables)
  }

  /** The parameters that differ between two plugins, or the plugin type if that differs. */
  private def plugins(before: AnyPlugin, after: AnyPlugin, prefix: String = "")(implicit context: PluginContext): Seq[ChangeDetail] = {
    if(before.pluginSpec.id != after.pluginSpec.id) {
      changed(s"${prefix}type", before.pluginSpec.label, after.pluginSpec.label)
    } else {
      after.pluginSpec.parameters.flatMap(parameter(_, before, after, prefix))
    }
  }

  private def parameter(param: PluginParameter, before: AnyPlugin, after: AnyPlugin, prefix: String)
                       (implicit context: PluginContext): Seq[ChangeDetail] = {
    val label = prefix + param.label
    val (previous, current) = (param(before), param(after))
    def named = if(previous != current) Seq(ChangeDetail(s"$label changed")) else Seq.empty
    param.parameterType match {
      case StringParameterType.PasswordParameterType =>
        named
      case _: StringParameterType[_] =>
        (resourceName(previous), resourceName(current)) match {
          // A resource path is relative to the project resources, which the change does not hold, so the name stands in
          case (Some(_), Some(_)) if previous == current => Seq.empty
          case (Some(b), Some(a)) if b != a => changed(label, b, a)
          case (Some(_), Some(_)) => named
          case _ => changed(label, render(param, before), render(param, after))
        }
      case objectType: PluginObjectParameterTypeTrait if objectType.pluginDescription.isDefined =>
        (previous, current) match {
          case (b: AnyPlugin, a: AnyPlugin) => plugins(b, a, s"$label / ")
          case _ => named
        }
      case _ =>
        named
    }
  }

  /** The name of a resource-valued parameter, or None for any other value. */
  private def resourceName(value: AnyRef): Option[String] = value match {
    case resource: Resource => Some(resource.name)
    case option: ResourceOption => Some(option.resource.map(_.name).getOrElse(""))
    case _ => None
  }

  /** A parameter value for display: its template if set, else its value. */
  private def render(param: PluginParameter, plugin: AnyPlugin)(implicit context: PluginContext): String = {
    plugin.templateValues.getOrElse(param.name, param.stringValue(plugin))
  }

  private def changed(label: String, previous: String, current: String): Seq[ChangeDetail] = {
    ChangeDetail.changed(label, VariableChanges.shorten(previous), VariableChanges.shorten(current))
  }

  /** The execution variables that differ, with their values unless sensitive. */
  private def variables(before: TemplateVariables, after: TemplateVariables): Seq[ChangeDetail] = {
    def label(variable: TemplateVariable): String = s"execution variable '${variable.name}'"
    def value(variable: TemplateVariable): Option[String] = {
      Some(VariableChanges.shorten(variable.template.filter(_.nonEmpty).getOrElse(variable.value)))
    }
    VariableChanges.diff(before, after).map {
      case SetVariable(None, variable) if variable.isSensitive => ChangeDetail(s"${label(variable)} added")
      case SetVariable(None, variable) => ChangeDetail(label(variable), after = value(variable))
      case SetVariable(Some(previous), variable) if VariableChanges.sensitive(Seq(previous, variable)) => ChangeDetail(s"${label(variable)} changed")
      case SetVariable(Some(previous), variable) => ChangeDetail(label(variable), value(previous), value(variable))
      case RemoveVariable(variable) if variable.isSensitive => ChangeDetail(s"${label(variable)} removed")
      case RemoveVariable(variable) => ChangeDetail(label(variable), before = value(variable))
      case other => ChangeDetail(other.describe)
    }
  }

  /** Whether two tasks hold the same data, execution variables and metadata, ignoring timestamps and users. */
  def same(task1: Task[TaskSpec], task2: Task[TaskSpec]): Boolean = {
    task1.data == task2.data &&
      task1.metaData.withoutUserData == task2.metaData.withoutUserData &&
      task1.executionVariables == task2.executionVariables
  }

  /** The project task in the state of `expected`; throws a conflict if it is missing or has changed since. */
  private[changes] def expectState(project: Project, expected: PlainTask[TaskSpec])
                                  (implicit userContext: UserContext): ProjectTask[TaskSpec] = {
    val task = project.anyTaskOption(expected.id)
      .getOrElse(throw ChangeConflictException(s"Task '${expected.labelOrId}' does not exist in project '${project.id}'."))
    if(!same(task, expected)) {
      throw ChangeConflictException(s"Task '${expected.labelOrId}' in project '${project.id}' has been changed since.")
    }
    task.asInstanceOf[ProjectTask[TaskSpec]]
  }
}
