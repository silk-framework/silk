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

/** A file was written that did not exist. Reverting deletes it while it is unchanged. */
case class ResourceCreated(path: String, after: FileState) extends RecordedChange {

  override def describe: String = s"Added file '$path'${ResourceChanges.sizeInfo(after)}"

  override def inverse: Option[Change] = Some(DeleteResource(path, after))
}

/** An existing file was overwritten or appended to. Its previous content is not kept, so it cannot be reverted. */
case class ResourceOverwritten(path: String, before: FileState, after: FileState) extends RecordedChange {

  override def describe: String = s"Overwrote file '$path'${ResourceChanges.sizeInfo(before, after)}"

  override def inverse: Option[Change] = None
}

/** A file was deleted. Its content is not kept, so it cannot be reverted. */
case class ResourceDeleted(path: String, before: FileState) extends RecordedChange {

  override def describe: String = s"Deleted file '$path'${ResourceChanges.sizeInfo(before)}"

  override def inverse: Option[Change] = None
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

  /** The file size for display, e.g. " (2.1 KB)", or empty if unknown. */
  def sizeInfo(state: FileState): String = state.size.map(size => s" (${formatSize(size)})").getOrElse("")

  /** The size change for display, e.g. " (1.8 KB → 2.1 KB)"; what is known of both sizes, or empty. */
  def sizeInfo(before: FileState, after: FileState): String = (before.size, after.size) match {
    case (Some(previous), Some(current)) => s" (${formatSize(previous)} → ${formatSize(current)})"
    case _ => sizeInfo(after)
  }

  private def formatSize(bytes: Long): String = {
    val units = Seq("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble
    var unit = 0
    while(value >= 1024 && unit < units.size - 1) { value /= 1024; unit += 1 }
    if(unit == 0) s"$bytes B" else String.format(java.util.Locale.ROOT, "%.1f %s", value, units(unit))
  }

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
}
