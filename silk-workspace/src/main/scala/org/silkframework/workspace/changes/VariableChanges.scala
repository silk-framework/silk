package org.silkframework.workspace.changes

import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.templating.exceptions.{CannotDeleteUsedVariableException, CannotDeleteVariableUsedByTaskException}
import org.silkframework.runtime.templating.{TemplateVariable, TemplateVariables}
import org.silkframework.runtime.validation.{RequestException, ValidationException}
import org.silkframework.workspace.Project
import org.silkframework.workspace.variables.{DeleteVariableModification, UpdateVariableModification}

/**
  * Sets a project variable, adding it if `before` is empty. Applies only while the variable is unchanged since.
  * The tasks whose templates use the variable follow it, both ways.
  */
case class SetVariable(before: Option[TemplateVariable], after: TemplateVariable) extends Change {

  // The value of a sensitive variable is never printed.
  override def summary: String = (before, VariableChanges.sensitive(before.toSeq :+ after)) match {
    case (None, true) => s"Added variable '${after.name}'"
    case (None, false) => s"Added variable '${after.name}' = ${VariableChanges.render(after)}"
    case (Some(_), true) => s"Set variable '${after.name}'"
    case (Some(previous), false) => s"Set variable '${after.name}': ${VariableChanges.render(previous)} → ${VariableChanges.render(after)}"
  }

  override def inverse: Option[Change] = Some(before match {
    case Some(previous) => SetVariable(Some(after), previous)
    case None => RemoveVariable(after)
  })

  override def applyTo(project: Project)(implicit userContext: UserContext): Unit = {
    VariableChanges.expect(project, after.name, before)
    VariableChanges.modify(UpdateVariableModification(project, after).execute())
  }

  override def conflict(context: ConflictContext)(implicit userContext: UserContext): Option[String] = {
    Change.conflictOf(VariableChanges.expect(context.project, after.name, before))
  }

  // The values may be sensitive, so they are never printed.
  override def toString: String = s"SetVariable(${after.name})"
}

/** Removes a project variable. Holds the variable, so the removal can be reverted; the variable is re-added at the end. */
case class RemoveVariable(variable: TemplateVariable) extends Change {

  override def summary: String = {
    if(variable.isSensitive) s"Removed variable '${variable.name}'"
    else s"Removed variable '${variable.name}' (${VariableChanges.render(variable)})"
  }

  override def inverse: Option[SetVariable] = Some(SetVariable(None, variable))

  override def applyTo(project: Project)(implicit userContext: UserContext): Unit = {
    VariableChanges.expect(project, variable.name, Some(variable))
    // The modification checks the uses itself; its refusal is told in the journal's words.
    VariableChanges.modify {
      try {
        DeleteVariableModification(project, variable.name).execute()
      } catch {
        case ex: CannotDeleteUsedVariableException => throw stillUsed(project, ex.dependentVariables.map(name => s"variable '$name'"))
        case ex: CannotDeleteVariableUsedByTaskException => throw stillUsed(project, Seq(s"task '${ex.task.labelOrId}'"))
      }
    }
  }

  override def conflict(context: ConflictContext)(implicit userContext: UserContext): Option[String] = {
    Change.conflictOf(expectRemovable(context))
  }

  // Unchanged since and used by no other variable or task: what the modification refuses for, checked without it.
  private def expectRemovable(context: ConflictContext)(implicit userContext: UserContext): Unit = {
    val project = context.project
    VariableChanges.expect(project, variable.name, Some(variable))
    val modification = DeleteVariableModification(project, variable.name)
    val users = modification.dependentVariables().map(name => s"variable '$name'") ++
      modification.invalidTasks(context.templatedTasks).map(task => s"task '${task.labelOrId}'")
    if(users.nonEmpty) {
      throw stillUsed(project, users)
    }
  }

  private def stillUsed(project: Project, users: Seq[String]): ChangeConflictException = {
    ChangeConflictException(s"Variable '${variable.name}' in project '${project.id}' is still used by ${users.mkString(", ")}.")
  }

  override def toString: String = s"RemoveVariable(${variable.name})"
}

private[workspace] object VariableChanges {

  /** The authored value of a variable for display: its template if set, else its value, shortened. */
  def render(variable: TemplateVariable): String = {
    variable.template.filter(_.nonEmpty) match {
      case Some(template) => s"template '${shorten(template)}'"
      case None => s"'${shorten(variable.value)}'"
    }
  }

  /** Whether any of the variables is sensitive, i.e. its value must not be shown. */
  def sensitive(variables: Seq[TemplateVariable]): Boolean = variables.exists(_.isSensitive)

  /** A value for display, cut to `max` characters. */
  def shorten(value: String, max: Int = 50): String = if(value.length > max) value.take(max) + "…" else value

  /** The changes that turn `before` into `after`: added and changed variables in the order of `after`, then the removed ones. */
  def diff(before: TemplateVariables, after: TemplateVariables): Seq[Change] = {
    val set =
      for(variable <- after.variables; previous = before.map.get(variable.name) if !previous.exists(same(_, variable))) yield {
        SetVariable(previous, variable)
      }
    val removed = for(variable <- before.variables if !after.map.contains(variable.name)) yield RemoveVariable(variable)
    set ++ removed
  }

  /**
    * Whether two variables are the same as authored: the value of a templated variable is derived and ignored;
    * an empty template or description equals a missing one (JSON input keeps them, the XML store drops them).
    */
  def same(variable1: TemplateVariable, variable2: TemplateVariable): Boolean = authored(variable1) == authored(variable2)

  private def authored(variable: TemplateVariable): TemplateVariable = {
    val template = variable.template.filter(_.nonEmpty)
    variable.copy(value = if(template.isDefined) "" else variable.value, template = template,
      description = variable.description.filter(_.nonEmpty))
  }

  /** Checks that the project's variable of that name is absent if `expected` is empty, else unchanged. */
  def expect(project: Project, name: String, expected: Option[TemplateVariable]): Unit = {
    (project.templateVariables.all.map.get(name), expected) match {
      case (None, None) =>
      case (Some(current), Some(variable)) if same(current, variable) =>
      case (None, Some(_)) =>
        throw ChangeConflictException(s"Variable '$name' does not exist in project '${project.id}'.")
      case (Some(_), None) =>
        throw ChangeConflictException(s"Variable '$name' already exists in project '${project.id}'.")
      case _ =>
        throw ChangeConflictException(s"Variable '$name' in project '${project.id}' has been changed since.")
    }
  }

  /** Runs a modification; one that it refuses, e.g. because a task would break, is a conflict. */
  def modify(body: => Unit): Unit = {
    try {
      body
    } catch {
      case ex: ChangeConflictException => throw ex
      case ex: RequestException => throw ChangeConflictException(ex.getMessage)
      case ex: ValidationException => throw ChangeConflictException(ex.getMessage)
    }
  }
}
