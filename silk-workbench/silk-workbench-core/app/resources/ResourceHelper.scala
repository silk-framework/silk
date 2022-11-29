package resources

import org.silkframework.config.TaskSpec
import org.silkframework.runtime.activity.UserContext
import org.silkframework.util.Identifier
import org.silkframework.workspace.{Project, ProjectTask}

import scala.collection.mutable
import scala.util.Try

object ResourceHelper {
  /** Refresh all caches that depend changes of a specific resource. */
  def refreshCachesOfDependingTasks(resourceName: String,
                                    project: Project)
                                   (implicit userContext: UserContext): Unit = {
    // The tasks depending on the resource that were actually updated.
    val updatedResourceTasks = mutable.Set[Identifier]()
    tasksDependingOnResource(resourceName, project).foreach { task =>
      var cacheUpdated = false
      task.activities.foreach { activity =>
        if (activity.isDatasetRelatedCache) {
          updatedResourceTasks.add(task.id)
          activity.startDirty()
          cacheUpdated = true
        }
      }
      // Also update path caches of tasks that directly depend on any of the updated tasks
      if(cacheUpdated) {
        task.dataValueHolder.republish()
      }
    }
  }

  /** Find all tasks that depend on a resource. */
  def tasksDependingOnResource(resourceName: String, project: Project)
                              (implicit userContext: UserContext): Seq[ProjectTask[_ <: TaskSpec]] = {
    project.allTasks
      .filter(_.referencedResources.map(_.name).contains(resourceName))
  }
}
