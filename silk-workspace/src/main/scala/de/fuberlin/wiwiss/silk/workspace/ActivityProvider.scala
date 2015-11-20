package de.fuberlin.wiwiss.silk.workspace

import de.fuberlin.wiwiss.silk.runtime.plugin.AnyPlugin
import de.fuberlin.wiwiss.silk.workspace.modules.{Task, TaskActivity}

import scala.reflect.ClassTag

trait ActivityProvider extends AnyPlugin {

  def projectActivities(project: Project): Seq[ProjectActivity] = Seq.empty

  def taskActivities[T: ClassTag](project: Project, task: Task[T]): Seq[TaskActivity[_,_]] = Seq.empty

}
