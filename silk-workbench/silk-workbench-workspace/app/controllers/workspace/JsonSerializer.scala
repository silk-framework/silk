package controllers.workspace

import java.io.File
import java.util.logging.LogRecord

import org.silkframework.config.{CustomTask, TaskSpec}
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.rule.{LinkSpec, TransformSpec}
import org.silkframework.runtime.activity.{HasValue, UserContext}
import org.silkframework.runtime.plugin.PluginDescription
import org.silkframework.runtime.resource.{Resource, ResourceManager}
import org.silkframework.runtime.serialization.WriteContext
import org.silkframework.serialization.json.ActivitySerializers.ExtendedStatusJsonFormat
import org.silkframework.serialization.json.JsonSerializers
import org.silkframework.workspace.activity.WorkspaceActivity
import org.silkframework.workspace.activity.workflow.Workflow
import org.silkframework.workspace.{Project, ProjectMarshallingTrait, ProjectTask, WorkspaceFactory}
import play.api.libs.json._
import JsonSerializers._

import scala.reflect.ClassTag

/**
  * Generates a JSON describing the current workspace
  */
object JsonSerializer {

  def projectsJson(implicit userContext: UserContext): JsArray = {
    JsArray(
      for (project <- WorkspaceFactory().workspace.projects) yield {
        projectJson(project)
      }
    )
  }

  def projectJson(project: Project)
                 (implicit userContext: UserContext): JsObject = {
    Json.obj(
      "name" -> JsString(project.name),
      "metaData" -> JsonSerializers.toJson(project.config.metaData),
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

  def projectResources(project: Project): JsArray = {
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

  def resourceProperties(resource: Resource, pathPrefix: String = ""): JsValue = {
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

  def projectActivities(project: Project): JsValue = {
    JsArray(
      for (activity <- project.activities) yield {
        workspaceActivity(activity)
      }
    )
  }

  def taskActivities(task: ProjectTask[_ <: TaskSpec]): JsValue = {
    JsArray(
      for (activity <- task.activities) yield {
        workspaceActivity(activity)
      }
    )
  }

  def workspaceActivity(activity: WorkspaceActivity[_]): JsValue = {
    Json.obj(
      "name" -> activity.name.toString,
      "instances" ->
        JsArray(
          for (control <- activity.allInstances.keys.toSeq) yield {
            Json.obj(
              "id" -> control.toString
            )
          }
        )
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
    implicit val writeContext = WriteContext[JsValue]()
    new ExtendedStatusJsonFormat(activity).write(activity.status())
  }

  def logRecords(records: Seq[LogRecord]): JsArray = {
    JsArray(records.map(logRecord))
  }

  def logRecord(record: LogRecord): JsObject = {
    JsObject(
      ("activity" -> JsString(record.getLoggerName.substring(record.getLoggerName.lastIndexOf('.') + 1))) ::
      ("level" -> JsString(record.getLevel.getName)) ::
      ("message" -> JsString(record.getMessage)) ::
      ("timestamp" -> JsNumber(record.getMillis)) :: Nil
    )
  }

  def pluginConfig(pluginConfig: PluginDescription[_]): JsObject = {
    JsObject(
      ("id" -> JsString(pluginConfig.id)) ::
      ("label" -> JsString(pluginConfig.label)) ::
      ("description" -> JsString(pluginConfig.description)) :: Nil
    )
  }

  def marshaller(marshaller: ProjectMarshallingTrait): JsObject = {
    JsObject(
      ("id" -> JsString(marshaller.id)) ::
      ("label" -> JsString(marshaller.name)) :: Nil
    )
  }
}
