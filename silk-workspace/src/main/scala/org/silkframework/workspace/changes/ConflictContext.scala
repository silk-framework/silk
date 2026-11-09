package org.silkframework.workspace.changes

import org.silkframework.config.TaskSpec
import org.silkframework.runtime.activity.UserContext
import org.silkframework.util.{Identifier, Uri}
import org.silkframework.workspace.variables.AffectableTasks
import org.silkframework.workspace.{Project, ProjectTask, ReferencingTask, TaskReferences}

/**
  * What the conflict checks of one request share: the project-wide facts a check would otherwise gather per entry,
  * each gathered once on first use. It reflects the project at that moment, so it is made per request.
  */
class ConflictContext(val project: Project)(implicit userContext: UserContext) {

  /** The tasks that reference each task, by the id of the referenced task, as a removal sees them. */
  lazy val referencingTasks: Map[Identifier, Seq[ReferencingTask]] = TaskReferences.of(project.allTasks)

  /** The tasks a variable removal can invalidate, with what its check evaluates of them, and the variables as the removal sees them. */
  lazy val affectableTasks: AffectableTasks = AffectableTasks.of(project)

  /** The tasks that have each tag, by the URI of the tag. */
  lazy val tasksByTag: Map[Uri, Seq[ProjectTask[_ <: TaskSpec]]] = {
    project.allTasks.flatMap(task => task.metaData.tags.map(_ -> task)).groupMap(_._1)(_._2)
  }
}
