package org.silkframework.workspace.resources

import org.silkframework.config.TaskSpec
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.Resource
import org.silkframework.util.Identifier
import org.silkframework.workspace.{Project, ProjectTask}
import scala.collection.mutable

object CacheUpdaterHelper {

  /** Refresh all caches that depend changes of a specific resource. */
  def refreshCachesOfDependingTasks(resourcePath: String,
                                    project: Project)
                                   (implicit userContext: UserContext): Unit = {
    // The tasks depending on the resource that were actually updated.
    val resource = project.resources.getInPath(resourcePath)
    val updatedResourceTasks = mutable.Set[Identifier]()
    tasksDependingOnResource(resource, project).foreach { task =>
      // Updated caches
      var cacheUpdated = false
      task.activities.foreach { activity =>
        if (activity.isDatasetRelatedCache) {
          updatedResourceTasks.add(task.id)
          activity.startDirty()
          cacheUpdated = true
        }
      }
      // Also update path caches of tasks that directly depend on any of the updated tasks
      if (cacheUpdated) {
        task.dataValueHolder.republish()
      }
      // Publish resource update to TaskSpec
      task.data.resourceUpdated(resource)
    }
  }

  /** Find all tasks that depend on a resource. */
  def tasksDependingOnResource(resource: Resource, project: Project)
                              (implicit userContext: UserContext): Seq[ProjectTask[_ <: TaskSpec]] = {
    project.allTasks
      .filter(
        _.referencedResources.exists(ref =>
          ref.path == resource.path))
  }


}
