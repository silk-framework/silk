package org.silkframework.workspace.changes

import org.silkframework.config.{CustomTask, PlainTask, Task, TaskSpec}
import org.silkframework.dataset.DatasetSpec
import org.silkframework.rule.{LinkSpec, TransformSpec}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.types.ResourceOption
import org.silkframework.runtime.plugin.{AnyPlugin, PluginContext, PluginObjectParameterTypeTrait, PluginParameter, StringParameterType}
import org.silkframework.runtime.resource.Resource
import org.silkframework.runtime.templating.TemplateVariables
import org.silkframework.util.Identifier
import org.silkframework.workspace.activity.workflow.Workflow
import org.silkframework.workspace.{Project, ProjectTask}

/** Adds a task to the project. Recorded whenever a task is added. */
case class AddTask(task: PlainTask[TaskSpec]) extends Change with NamesTask {

  override def taskId: Identifier = task.id

  override def taskLabel: Option[String] = task.metaData.label

  override def describe: String = s"Added ${TaskChanges.kind(task.data)} '${task.labelOrId}'"

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

  override def taskLabel: Option[String] = task.metaData.label

  override def describe: String = s"Removed ${TaskChanges.kind(task.data)} '${task.labelOrId}'"

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
  override def taskLabel: Option[String] = after.metaData.label

  override def describe: String = {
    val renamed = if(after.labelOrId != before.labelOrId) s", renamed from '${before.labelOrId}'" else ""
    val details = TaskChanges.diff(before, after)
    val changed = if(details.isEmpty) "" else ": " + details.mkString(", ")
    s"Updated ${TaskChanges.kind(after.data)} '${after.labelOrId}'$renamed$changed"
  }

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

  /** At most this many details are listed for an update. */
  private val maxDetails = 5

  /**
    * What an update changed, for display: parameters by their labels with the previous and the new value, an object
    * parameter such as the mapping rules by name only, a dataset's own settings, the metadata and the execution
    * variables. Passwords and sensitive variables are never printed. Empty if nothing is detected, e.g. for a plugin
    * without value equality.
    */
  def diff(before: Task[TaskSpec], after: Task[TaskSpec]): Seq[String] = {
    implicit val context: PluginContext = PluginContext.empty
    val data = (before.data, after.data) match {
      case (b: DatasetSpec[_], a: DatasetSpec[_]) =>
        plugins(b.plugin, a.plugin) ++
          changed("URI attribute", quoted(b.uriAttribute.map(_.uri).getOrElse("")), quoted(a.uriAttribute.map(_.uri).getOrElse(""))) ++
          changed("Read-only", quoted(b.readOnly.toString), quoted(a.readOnly.toString))
      case (b: AnyPlugin, a: AnyPlugin) =>
        plugins(b, a)
      case _ =>
        Seq.empty
    }
    val metaData =
      Seq("description" -> (before.metaData.description != after.metaData.description),
          "tags" -> (before.metaData.tags != after.metaData.tags)).collect { case (field, true) => s"$field changed" }
    val details = data ++ metaData ++ variables(before.executionVariables, after.executionVariables)
    if(details.size > maxDetails) details.take(maxDetails) :+ s"and ${details.size - maxDetails} more" else details
  }

  /** The parameters that differ between two plugins, or the plugin type if that differs. */
  private def plugins(before: AnyPlugin, after: AnyPlugin, prefix: String = "")(implicit context: PluginContext): Seq[String] = {
    if(before.pluginSpec.id != after.pluginSpec.id) {
      Seq(s"${prefix}type '${before.pluginSpec.label}' → '${after.pluginSpec.label}'")
    } else {
      after.pluginSpec.parameters.flatMap(parameter(_, before, after, prefix))
    }
  }

  private def parameter(param: PluginParameter, before: AnyPlugin, after: AnyPlugin, prefix: String)
                       (implicit context: PluginContext): Seq[String] = {
    val label = prefix + param.label
    val (previous, current) = (param(before), param(after))
    param.parameterType match {
      case StringParameterType.PasswordParameterType =>
        if(previous != current) Seq(s"$label changed") else Seq.empty
      case _: StringParameterType[_] =>
        (resourceName(previous), resourceName(current)) match {
          // A resource path is relative to the project resources, which the change does not hold, so the name stands in
          case (Some(_), Some(_)) if previous == current => Seq.empty
          case (Some(b), Some(a)) if b != a => changed(label, quoted(b), quoted(a))
          case (Some(_), Some(_)) => Seq(s"$label changed")
          case _ => changed(label, render(param, before), render(param, after))
        }
      case objectType: PluginObjectParameterTypeTrait if objectType.pluginDescription.isDefined =>
        (previous, current) match {
          case (b: AnyPlugin, a: AnyPlugin) => plugins(b, a, s"$label / ")
          case _ => if(previous != current) Seq(s"$label changed") else Seq.empty
        }
      case _ =>
        if(previous != current) Seq(s"$label changed") else Seq.empty
    }
  }

  /** The name of a resource-valued parameter, or None for any other value. */
  private def resourceName(value: AnyRef): Option[String] = value match {
    case resource: Resource => Some(resource.name)
    case option: ResourceOption => Some(option.resource.map(_.name).getOrElse(""))
    case _ => None
  }

  /** A parameter value for display: its template if set, else its value, shortened. */
  private def render(param: PluginParameter, plugin: AnyPlugin)(implicit context: PluginContext): String = {
    plugin.templateValues.get(param.name) match {
      case Some(template) => s"template '${VariableChanges.shorten(template)}'"
      case None => quoted(param.stringValue(plugin))
    }
  }

  private def quoted(value: String): String = s"'${VariableChanges.shorten(value)}'"

  private def changed(label: String, previous: String, current: String): Seq[String] = {
    if(previous != current) Seq(s"$label $previous → $current") else Seq.empty
  }

  /** The execution variables that differ, with their values unless sensitive. */
  private def variables(before: TemplateVariables, after: TemplateVariables): Seq[String] = {
    VariableChanges.diff(before, after).map {
      case SetVariable(None, variable) =>
        s"execution variable '${variable.name}' added" + (if(variable.isSensitive) "" else s" = ${VariableChanges.render(variable)}")
      case SetVariable(Some(previous), variable) if VariableChanges.sensitive(Seq(previous, variable)) =>
        s"execution variable '${variable.name}' changed"
      case SetVariable(Some(previous), variable) =>
        s"execution variable '${variable.name}' ${VariableChanges.render(previous)} → ${VariableChanges.render(variable)}"
      case RemoveVariable(variable) =>
        s"execution variable '${variable.name}' removed"
      case other =>
        other.describe
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
