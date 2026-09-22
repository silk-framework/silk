package org.silkframework.workspace.changes

import org.silkframework.config.TaskSpec
import org.silkframework.runtime.activity.UserContext
import org.silkframework.util.Identifier
import org.silkframework.workspace.variables.DeleteVariableModification
import org.silkframework.workspace.{Project, ProjectTask, ReferencingTask, TaskReferences}

/**
  * What the conflict checks of one listing share: the project-wide facts a check would otherwise gather per entry,
  * each gathered once on first use. It reflects the project at that moment, so it is made per listing.
  */
class ConflictContext(val project: Project)(implicit userContext: UserContext) {

  /** The tasks that reference each task, by the id of the referenced task, as a removal sees them. */
  lazy val referencingTasks: Map[Identifier, Seq[ReferencingTask]] = TaskReferences.of(project.allTasks)

  /** The tasks a variable removal can invalidate, as the removal itself sees them. */
  lazy val templatedTasks: Seq[ProjectTask[_ <: TaskSpec]] = DeleteVariableModification.affectableTasks(project)
}
