package controllers.workspace

import org.silkframework.config.{CustomTask, TaskSpec}
import org.silkframework.dataset.DatasetSpec.GenericDatasetSpec
import org.silkframework.rule.{LinkSpec, TransformSpec}
import org.silkframework.runtime.activity.{HasValue, UserContext}
import org.silkframework.runtime.serialization.WriteContext
import org.silkframework.serialization.json.ActivitySerializers.ExtendedStatusJsonFormat
import org.silkframework.serialization.json.JsonSerializers
import org.silkframework.serialization.json.MetaDataSerializers._
import org.silkframework.workspace.activity.WorkspaceActivity
import org.silkframework.workspace.activity.workflow.Workflow
import org.silkframework.workspace.{Project, ProjectMarshallingTrait, WorkspaceFactory}
import play.api.libs.json._

import java.util.logging.LogRecord
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
      "name" -> JsString(project.id),
      "metaData" -> JsonSerializers.toJsonEmptyContext(project.config.metaData),
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

  def activityConfig(config: Map[String, String]) = JsArray(
    for ((name, value) <- config.toSeq) yield
      Json.obj("name" -> name, "value" -> value)
  )

  def readActivityConfig(json: JsValue): Map[String, String] = {
    for (value <- json.as[JsArray].value) yield
      ((value \ "name").toString(), (value \ "value").toString)
  }.toMap


  def activityStatus(activity: WorkspaceActivity[_ <: HasValue]): JsValue = {
    implicit val writeContext = WriteContext.empty[JsValue]
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

  def marshaller(marshaller: ProjectMarshallingTrait): JsObject = {
    JsObject(
      ("id" -> JsString(marshaller.id)) ::
      ("label" -> JsString(marshaller.name)) ::
      ("description" -> JsString(marshaller.pluginSpec.description)) ::
      ("fileExtension" -> JsString(marshaller.fileExtension)) ::
      ("mediaType" -> marshaller.mediaType.map(JsString).orNull) :: Nil
    )
  }
}
