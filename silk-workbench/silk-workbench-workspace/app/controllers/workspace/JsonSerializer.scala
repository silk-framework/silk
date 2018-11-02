package controllers.workspace

import java.io.File
import java.util.logging.LogRecord

import org.silkframework.config.{CustomTask, Task, TaskSpec}
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.dataset.{Dataset, DatasetSpec}
import org.silkframework.entity.EntitySchema
import org.silkframework.rule.{LinkSpec, TransformSpec}
import org.silkframework.runtime.activity.{HasValue, Status}
import org.silkframework.runtime.activity.{Status, UserContext}
import org.silkframework.runtime.plugin.PluginDescription
import org.silkframework.runtime.resource.{Resource, ResourceManager}
import org.silkframework.runtime.serialization.WriteContext
import org.silkframework.serialization.json.JsonSerializers.MetaDataJsonFormat
import org.silkframework.workspace.activity.workflow.Workflow
import org.silkframework.workspace.activity.{ProjectActivity, TaskActivity, WorkspaceActivity}
import org.silkframework.workspace.{Project, ProjectMarshallingTrait, ProjectTask, WorkspaceFactory}
import play.api.libs.json._

import scala.reflect.ClassTag

/**
  * Generates a JSON describing the current workspace
  */
object JsonSerializer {

  def projectsJson(implicit userContext: UserContext) = {
    JsArray(
      for (project <- WorkspaceFactory().workspace.projects) yield {
        projectJson(project)
      }
    )
  }

  def projectJson(project: Project)
                 (implicit userContext: UserContext)= {
    Json.obj(
      "name" -> JsString(project.name),
      "tasks" -> Json.obj(
      "dataset" -> tasksJson[GenericDatasetSpec](project),
      "transform" -> tasksJson[TransformSpec](project),
      "linking" -> tasksJson[LinkSpec](project),
      "workflow" -> tasksJson[Workflow](project),
      "custom" -> tasksJson[CustomTask](project)
      )
    )
  }

  def tasksJson[T <: TaskSpec : ClassTag](project: Project)
                                         (implicit userContext: UserContext)= JsArray(
    for (task <- project.tasks[T]) yield {
      JsString(task.id)
    }
  )

  def projectResources(project: Project) = {
    JsArray(resourcesArray(project.resources))
  }

  def resourcesArray(resources: ResourceManager, pathPrefix: String = ""): Seq[JsValue] = {
    val directResources =
      for(resourceName <- resources.list) yield
        resourceProperties(resources.get(resourceName), pathPrefix)

    val childResources =
      for(resourceName <- resources.listChildren) yield
        resourcesArray(resources.child(resourceName), pathPrefix + resourceName + File.separator)

    directResources ++ childResources.flatten
  }

  def resourceProperties(resource: Resource, pathPrefix: String = "") = {
    val sizeValue = resource.size match {
      case Some(size) => JsNumber(BigDecimal.decimal(size))
      case None => JsNull
    }

    val modificationValue = resource.modificationTime match {
      case Some(time) => JsString(time.toString)
      case None => JsNull
    }

    Json.obj(
      "name" -> resource.name,
      "relativePath" -> JsString(pathPrefix + resource.name),
      "absolutePath" -> resource.path,
      "size" -> sizeValue,
      "modified" -> modificationValue
    )
  }

  def projectActivities(project: Project) = JsArray(
    for (activity <- project.activities) yield {
      JsString(activity.name)
    }
  )

  def taskActivities(task: ProjectTask[_ <: TaskSpec]): JsValue = {
    JsArray(
      for (activity <- task.activities) yield {
        Json.obj(
          "name" -> activity.name.toString,
          "controls" ->
            JsArray(
              for (control <- activity.allControls.keys.toSeq) yield {
                Json.obj(
                  "id" -> control.toString
                )
              }
            )
        )
      }
    )
  }

  def activityConfig(config: Map[String, String]) = JsArray(
    for ((name, value) <- config.toSeq) yield
      Json.obj("name" -> name, "value" -> value)
  )

  def readActivityConfig(json: JsValue): Map[String, String] = {
    for (value <- json.as[JsArray].value) yield
      ((value \ "name").toString(), (value \ "value").toString)
  }.toMap


  def activityStatus(activity: WorkspaceActivity[_ <: HasValue]): JsValue = {
    activityStatus(activity.project.name, activity.taskOption.map(_.id.toString).getOrElse(""), activity.name, activity.control.status(), activity.startTime)
  }

  def activityStatus(project: String, task: String, activity: String, status: Status, startTime: Option[Long]): JsValue = {
    JsObject(
      ("project" -> JsString(project)) ::
      ("task" -> JsString(task)) ::
      ("activity" -> JsString(activity)) ::
      ("statusName" -> JsString(status.name)) ::
      ("isRunning" -> JsBoolean(status.isRunning)) ::
      ("progress" -> JsNumber(status.progress * 100.0)) ::
      ("message" -> JsString(status.toString)) ::
      ("failed" -> JsBoolean(status.failed)) ::
      ("lastUpdateTime" -> JsNumber(status.timestamp)) ::
      ("startTime" -> startTime.map(JsNumber(_)).getOrElse(JsNull)) :: Nil
    )
  }

  def logRecords(records: Seq[LogRecord]) = {
    JsArray(records.map(logRecord))
  }

  def logRecord(record: LogRecord) = {
    JsObject(
      ("activity" -> JsString(record.getLoggerName.substring(record.getLoggerName.lastIndexOf('.') + 1))) ::
      ("level" -> JsString(record.getLevel.getName)) ::
      ("message" -> JsString(record.getMessage)) ::
      ("timestamp" -> JsNumber(record.getMillis)) :: Nil
    )
  }

  def pluginConfig(pluginConfig: PluginDescription[_]) = {
    JsObject(
      ("id" -> JsString(pluginConfig.id)) ::
      ("label" -> JsString(pluginConfig.label)) ::
      ("description" -> JsString(pluginConfig.description)) :: Nil
    )
  }

  def marshaller(marshaller: ProjectMarshallingTrait) = {
    JsObject(
      ("id" -> JsString(marshaller.id)) ::
      ("label" -> JsString(marshaller.name)) :: Nil
    )
  }
}
