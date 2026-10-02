package org.silkframework.workspace.changes

import org.silkframework.config.Tag
import org.silkframework.runtime.activity.UserContext
import org.silkframework.util.Uri
import org.silkframework.workspace.Project

/**
  * Sets a tag of the project, adding it if `before` is empty. Applies only while the tag is unchanged since.
  * The tasks that have the tag hold its URI, so they follow a new label.
  */
case class SetTag(before: Option[Tag], after: Tag) extends Change {

  override def summary: String = before match {
    case None => s"Added tag '${TagChanges.name(after)}'"
    case Some(previous) => s"Renamed tag '${TagChanges.name(previous)}' to '${TagChanges.name(after)}'"
  }

  override def inverse: Option[Change] = Some(before match {
    case Some(previous) => SetTag(Some(after), previous)
    case None => RemoveTag(after)
  })

  override def applyTo(project: Project)(implicit userContext: UserContext): Unit = project.synchronized {
    TagChanges.expect(project, after.uri, before)
    project.tagManager.putTag(after)
  }

  override def conflict(context: ConflictContext)(implicit userContext: UserContext): Option[String] = {
    Change.conflictOf(TagChanges.expect(context.project, after.uri, before))
  }
}

/**
  * Removes a tag of the project. Applies only while the tag is unchanged since and neither a task nor the project
  * itself has it, as it would stay there as a URI without a label. Holds the tag, so the removal can be reverted.
  */
case class RemoveTag(tag: Tag) extends Change {

  override def summary: String = s"Removed tag '${TagChanges.name(tag)}'"

  override def inverse: Option[SetTag] = Some(SetTag(None, tag))

  // The task writes of Project and of the journal take the project monitor as well, so none slips in between the check
  // and the removal. A direct ProjectTask update, as the REST metadata endpoint makes, can.
  override def applyTo(project: Project)(implicit userContext: UserContext): Unit = project.synchronized {
    expectRemovable(project)
    project.tagManager.deleteTag(tag.uri)
  }

  override def conflict(context: ConflictContext)(implicit userContext: UserContext): Option[String] = {
    Change.conflictOf(expectRemovable(context.project))
  }

  private def expectRemovable(project: Project)(implicit userContext: UserContext): Unit = {
    TagChanges.expect(project, tag.uri, Some(tag))
    // With the id, as the label alone does not tell which task to change.
    val users = project.allTasks.filter(_.metaData.tags.contains(tag.uri)).map(task => s"task ${task.labelAndId}") ++
      Seq("the project itself").filter(_ => project.config.metaData.tags.contains(tag.uri))
    if(users.nonEmpty) {
      throw ChangeConflictException(s"Tag '${TagChanges.name(tag)}' in project '${project.id}' is still used by ${users.mkString(", ")}.")
    }
  }
}

private[workspace] object TagChanges {

  /** Names a tag for display: its label, shortened. */
  def name(tag: Tag): String = VariableChanges.shorten(tag.label)

  /** Checks that the project's tag of that URI is absent if `expected` is empty, else unchanged. */
  def expect(project: Project, uri: Uri, expected: Option[Tag])(implicit userContext: UserContext): Unit = {
    (project.tagManager.allTags().find(_.uri == uri), expected) match {
      case (None, None) =>
      case (Some(current), Some(tag)) if current == tag =>
      case (None, Some(tag)) =>
        throw ChangeConflictException(s"Tag '${name(tag)}' does not exist in project '${project.id}'.")
      case (Some(current), None) =>
        throw ChangeConflictException(s"Tag '${name(current)}' already exists in project '${project.id}'.")
      case (Some(current), Some(_)) =>
        throw ChangeConflictException(s"Tag '${name(current)}' in project '${project.id}' has been changed since.")
    }
  }
}
