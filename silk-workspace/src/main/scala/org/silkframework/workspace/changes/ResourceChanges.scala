package org.silkframework.workspace.changes

import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.{Resource, WritableResource}
import org.silkframework.workspace.Project

import java.time.Instant

/** The size and modification time of a file, by which a change checks that the file is unchanged since. */
case class FileState(size: Option[Long], modified: Option[Instant])

object FileState {

  /** The state of a resource, or None if it does not exist. */
  def of(resource: Resource): Option[FileState] = {
    if(resource.exists) Some(FileState(resource.size, resource.modificationTime)) else None
  }
}

/**
  * A file was written that did not exist. Recorded by the resource funnel from the outcome of the write, so it holds
  * no content and is not applied itself; its inverse is.
  */
case class ResourceCreated(path: String, after: FileState) extends Change {

  override def describe: String = s"Added file '$path'"

  override def inverse: Option[Change] = Some(DeleteResource(path, after))

  override def applyTo(project: Project)(implicit userContext: UserContext): Unit = ResourceChanges.recordedOnly(this)
}

/** An existing file was overwritten or appended to. Its previous content is not kept, so it cannot be reverted. */
case class ResourceOverwritten(path: String, before: FileState, after: FileState) extends Change {

  override def describe: String = s"Overwrote file '$path'"

  override def inverse: Option[Change] = None

  override def applyTo(project: Project)(implicit userContext: UserContext): Unit = ResourceChanges.recordedOnly(this)
}

/** A file was deleted. Its content is not kept, so it cannot be reverted. */
case class ResourceDeleted(path: String, before: FileState) extends Change {

  override def describe: String = s"Deleted file '$path'"

  override def inverse: Option[Change] = None

  override def applyTo(project: Project)(implicit userContext: UserContext): Unit = ResourceChanges.recordedOnly(this)
}

/**
  * Deletes a file that is in the expected state: the inverse of a creation. Applied through the resource funnel,
  * which records the deletion; never recorded itself.
  */
case class DeleteResource(path: String, expected: FileState) extends Change {

  override def describe: String = s"Deleted file '$path'"

  override def inverse: Option[Change] = None

  override def applyTo(project: Project)(implicit userContext: UserContext): Unit = {
    ResourceChanges.expect(project, path, expected).delete()
  }
}

private[workspace] object ResourceChanges {

  /** The project's file at that path, which must be in the expected state. */
  def expect(project: Project, path: String, expected: FileState): WritableResource = {
    val resource = project.resources.getInPath(path)
    FileState.of(resource) match {
      case Some(current) if current == expected =>
      case None =>
        throw ChangeConflictException(s"File '$path' does not exist in project '${project.id}'.")
      case _ =>
        throw ChangeConflictException(s"File '$path' in project '${project.id}' has been changed since.")
    }
    resource
  }

  def recordedOnly(change: Change): Nothing = {
    throw new IllegalStateException(s"${change.changeType} records the outcome of a write and cannot be applied; its inverse can.")
  }
}
