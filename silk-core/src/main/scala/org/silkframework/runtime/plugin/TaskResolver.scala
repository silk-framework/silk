package org.silkframework.runtime.plugin

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.Identifier
import org.silkframework.workspace.{ProjectTrait, WorkspaceReadTrait}

import scala.reflect.ClassTag

/** Resolves referenced tasks at execution time. */
trait TaskResolver {
  def resolveTyped[T <: TaskSpec : ClassTag](id: Identifier): Task[T]
}

object TaskResolver {
  val empty: TaskResolver = new TaskResolver {
    override def resolveTyped[T <: TaskSpec : ClassTag](id: Identifier): Task[T] = {
      throw new ValidationException(s"No task resolver available to resolve task '$id'.")
    }
  }

  def fromProjectId(projectId: Option[String], workspace: WorkspaceReadTrait)(implicit userContext: UserContext): TaskResolver = {
    projectId match {
      case Some(id) =>
        ProjectTraitTaskResolver(workspace.project(id), userContext)
      case None => throw new ValidationException("Cannot create task resolver because of missing project ID!")
    }
  }

  def fromProject(project: ProjectTrait)(implicit userContext: UserContext): TaskResolver = {
    ProjectTraitTaskResolver(project, userContext)
  }

  private case class ProjectTraitTaskResolver(project: ProjectTrait, implicit private val userContext: UserContext) extends TaskResolver {
    override def resolveTyped[T <: TaskSpec : ClassTag](id: Identifier): Task[T] = project.task[T](id)
  }
}
