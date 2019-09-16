package org.silkframework.serialization.json

import org.silkframework.runtime.activity.Status
import org.silkframework.runtime.activity.Status.{Canceling, Finished, Idle, Running, Waiting}
import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
import org.silkframework.workspace.activity.WorkspaceActivity
import play.api.libs.json._

object ActivitySerializers {

  implicit object StatusJsonFormat extends JsonFormat[Status] {

    private final val STATUS_NAME = "statusName"
    private final val IS_RUNNING = "isRunning"
    private final val PROGRESS = "progress"
    private final val MESSAGE = "message"
    private final val FAILED = "failed"
    private final val LAST_UPDATE_TIME = "lastUpdateTime"
    private final val RUNTIME = "runtime"
    private final val CANCELLED = "cancelled"
    private final val EXCEPTION_MESSAGE = "exceptionMessage"

    private final val IDLE_TYPE = classOf[Idle].getSimpleName
    private final val WAITING_TYPE = classOf[Waiting].getSimpleName
    private final val RUNNING_TYPE = classOf[Running].getSimpleName
    private final val CANCELING_TYPE = classOf[Canceling].getSimpleName
    private final val FINISHED_TYPE = classOf[Finished].getSimpleName

    override def write(status: Status)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      val basicParameters =
        JsObject(
          (STATUS_NAME -> JsString(status.name)) ::
          (IS_RUNNING -> JsBoolean(status.isRunning)) ::
          (PROGRESS -> status.progress.map(p => JsNumber(p * 100.0)).getOrElse(JsNull)) ::
          (MESSAGE -> JsString(status.toString)) ::
          (FAILED -> JsBoolean(status.failed)) ::
          (LAST_UPDATE_TIME -> JsNumber(status.timestamp)) :: Nil
        )
      status match {
        case Finished(success, runtime, cancelled, exception) =>
          basicParameters +
            (RUNTIME -> JsNumber(runtime)) +
            (CANCELLED -> JsBoolean(cancelled)) +
            (EXCEPTION_MESSAGE -> exception.map(_.getMessage).map(JsString).getOrElse(JsNull))
        case _ =>
          basicParameters
      }
    }

    override def read(value: JsValue)(implicit readContext: ReadContext): Status = {
        (value \ STATUS_NAME).as[String] match {
        case IDLE_TYPE =>
          Idle()
        case WAITING_TYPE =>
          Waiting()
        case RUNNING_TYPE =>
          Running(
            message = (value \ MESSAGE).as[String],
            progress = (value \ PROGRESS).asOpt[Double]
          )
        case CANCELING_TYPE =>
          Canceling(
            progress = (value \ PROGRESS).asOpt[Double]
          )
        case FINISHED_TYPE =>
          Finished(
            success = !(value \ FAILED).as[Boolean],
            runtime = (value \ RUNTIME).as[Long],
            cancelled = (value \ CANCELLED).as[Boolean],
            // At the moment the stack trace gets lost during serialization:
            exception = (value \ EXCEPTION_MESSAGE).asOpt[String].map(msg => new Exception(msg))
          )
        case statusName: String =>
            throw new IllegalArgumentException("Unknown status type: " + statusName)
      }
    }
  }

  class ExtendedStatusJsonFormat(project: String, task: String, activity: String, startTime: Option[Long]) extends WriteOnlyJsonFormat[Status] {

    def this(activity: WorkspaceActivity[_]) = {
      this(activity.project.name, activity.taskOption.map(_.id.toString).getOrElse(""), activity.name, activity.startTime)
    }

    override def write(status: Status)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      StatusJsonFormat.write(status).as[JsObject] +
      ("project" -> JsString(project)) +
      ("task" -> JsString(task)) +
      ("activity" -> JsString(activity)) +
      ("startTime" -> startTime.map(JsNumber(_)).getOrElse(JsNull))
    }
  }

}
