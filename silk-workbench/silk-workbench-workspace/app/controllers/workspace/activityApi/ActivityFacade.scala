package controllers.workspace.activityApi

import controllers.workspace.activityApi.ActivityListResponse.{ActivityCharacteristics, ActivityInstance, ActivityListEntry, ActivityMetaData}
import org.silkframework.config.TaskSpec
import org.silkframework.rule.TransformSpec
import org.silkframework.runtime.activity.Status.Waiting
import org.silkframework.runtime.activity.{HasValue, UserContext}
import org.silkframework.runtime.plugin.ParameterValues
import org.silkframework.workspace.activity.WorkspaceActivity
import org.silkframework.workspace.activity.vocabulary.GlobalVocabularyCache
import org.silkframework.workspace.activity.workflow.Workflow
import org.silkframework.workspace.{ProjectTask, WorkspaceFactory}

import scala.util.Try

/**
  * Provides a simple API for accessing and controlling activities.
  */
object ActivityFacade {

  def listActivities(projectName: String,
                     taskName: String,
                     addDependentActivities: Boolean = false)
                    (implicit user: UserContext): Seq[ActivityListEntry] = {
    var mainActivities: Seq[String] = Seq.empty
    var dependentActivities: Seq[WorkspaceActivity[_ <: HasValue]] = Seq.empty
    val activities =
      if(projectName.nonEmpty) {
        val project = WorkspaceFactory().workspace.project(projectName)
        if(taskName.nonEmpty) {
          val task = project.anyTask(taskName)
          mainActivities = task.data.mainActivities
          if(addDependentActivities) {
            dependentActivities = taskActivityDependencies(task)
          }
          task.activities
        } else {
          project.activities
        }
      } else {
        WorkspaceFactory().workspace.activities
      }

    val entries = for(activity <- activities) yield {
      activityEntry(mainActivities, activity)
    }
    if(dependentActivities.nonEmpty) {
      val dependentEntries = for(activity <- dependentActivities) yield {
        dependentActivityEntry(mainActivities, activity)
      }
      entries ++ dependentEntries
    } else {
      entries
    }
  }

  private def activityEntry(mainActivities: Seq[String], activity: WorkspaceActivity[_ <: HasValue]): ActivityListEntry = {
    ActivityListEntry(
      name = activity.name.toString,
      label = activity.label,
      instances = activity.allInstances.keys.toSeq.map(id => ActivityInstance(id.toString)),
      activityCharacteristics = ActivityCharacteristics(
        isMainActivity = mainActivities.contains(activity.name.toString),
        isCacheActivity = activity.isCacheActivity
      ),
      metaData = None
    )
  }

  // Dependent activities can be from other tasks or on project or workspace level, thus they need this meta data to be handled.
  private def dependentActivityEntry(mainActivities: Seq[String], activity: WorkspaceActivity[_ <: HasValue]): ActivityListEntry = {
    ActivityListEntry(
      name = activity.name.toString,
      label = activity.label,
      instances = activity.allInstances.keys.toSeq.map(id => ActivityInstance(id.toString)),
      activityCharacteristics = ActivityCharacteristics(
        isMainActivity = mainActivities.contains(activity.name.toString),
        isCacheActivity = activity.isCacheActivity
      ),
      metaData = Some(ActivityMetaData(
        projectId = activity.projectOpt.map(_.id),
        taskId = activity.taskOption.map(_.id)
      ))
    )
  }

  // Fetch activities a task depends on.
  private def taskActivityDependencies(task: ProjectTask[_ <: TaskSpec])
                                      (implicit user: UserContext): Seq[WorkspaceActivity[_ <: HasValue]] = {
    // Some specific tasks have dependencies on activities not part of the task itself.
    var additionalActivities: List[WorkspaceActivity[_ <: HasValue]] = List.empty
    task.data match {
      case transformSpec: TransformSpec =>
        additionalActivities ::= WorkspaceFactory().workspace.activity[GlobalVocabularyCache]
      case _ =>
    }
    val typeCacheDependencies = typeCacheActivitiesOfInputs(task)
    additionalActivities ++ typeCacheDependencies
  }

  private def typeCacheActivitiesOfInputs(task: ProjectTask[_ <: TaskSpec])
                                         (implicit user: UserContext): Iterable[WorkspaceActivity[_ <: HasValue]] = {
    val inputTasks = if (!task.data.isInstanceOf[Workflow]) {
      // We don't want to show type caches for workflows, but for all other tasks, e.g. transform tasks
      task.inputTasks
    } else {
      Seq.empty
    }
    // For some type system reasons we cannot use a for expression here, thus the complicated approach.
    var typeCacheActivities: List[WorkspaceActivity[_ <: HasValue]] = Nil
    inputTasks foreach { inputTaskId =>
      task.project.anyTaskOption(inputTaskId) foreach { inputTask =>
        Try(inputTask.activity("TypesCache")) foreach { typeCacheActivity =>
          typeCacheActivities ::= typeCacheActivity
        }
      }
    }
    typeCacheActivities
  }

  def start(projectName: String,
            taskName: String,
            activityName: String,
            blocking: Boolean,
            activityConfig: Map[String, String])
           (implicit user: UserContext): StartActivityResponse = {
    val activity = getActivity(projectName, taskName, activityName)
    if (activity.isSingleton && activity.status().isRunning) {
      throw ActivityAlreadyRunningException(activityName)
    } else {
      val id =
        if (blocking) {
          activity.startBlocking(ParameterValues.fromStringMap(activityConfig))
        } else {
          activity.start(ParameterValues.fromStringMap(activityConfig))
        }
      StartActivityResponse(activityName, id.toString)
    }
  }

  def startPrioritized(projectName: String,
                       taskName: String,
                       activityName: String)
                      (implicit user: UserContext): StartActivityResponse = {
    val activity = getActivity(projectName, taskName, activityName)
    if (activity.isSingleton && activity.status().isRunning && !activity.status().isInstanceOf[Waiting]) {
      throw ActivityAlreadyRunningException(activityName)
    } else {
      val id = activity.startPrioritized()
      StartActivityResponse(activityName, id.toString)
    }
  }

  def getActivity(projectName: String,
                  taskName: String,
                  activityName: String)
                 (implicit user: UserContext): WorkspaceActivity[_ <: HasValue] = {
    val workspace = WorkspaceFactory.factory.workspace
    if(projectName.trim.isEmpty) {
      workspace.activity(activityName)
    } else {
      val project = workspace.project(projectName)
      if (taskName.nonEmpty) {
        project.anyTask(taskName).activity(activityName)
      } else {
        project.activity(activityName)
      }
    }
  }

}
