package org.silkframework.serialization.json

 import java.time.Instant

 import org.silkframework.runtime.activity.{ActivityExecutionMetaData, ActivityExecutionResult, Status}
 import org.silkframework.runtime.activity.Status.{Canceling, Finished, Idle, Running, Waiting}
 import org.silkframework.runtime.serialization.{ReadContext, WriteContext}
 import org.silkframework.runtime.users.SimpleUser
 import org.silkframework.workspace.activity.WorkspaceActivity
 import org.silkframework.serialization.json.JsonHelpers._
 import play.api.libs.json._

object ActivitySerializers {

  implicit object StatusJsonFormat extends JsonFormat[Status] {

    private final val STATUS_NAME = "statusName"
    final val CONCRETE_STATUS = "concreteStatus"
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
          (CONCRETE_STATUS -> JsString(status.concreteStatus)) ::
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

  class ExtendedStatusJsonFormat(project: String, task: String, activity: String, activityLabel: String, queueTime: => Option[Instant], startTime: => Option[Instant]) extends WriteOnlyJsonFormat[Status] {

    def this(activity: WorkspaceActivity[_]) = {
      this(
        activity.projectOpt.map(_.id.toString).getOrElse(""),
        activity.taskOption.map(_.id.toString).getOrElse(""),
        activity.name,
        activity.label,
        activity.queueTime,
        activity.startTime
      )
    }

    override def write(status: Status)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      StatusJsonFormat.write(status).as[JsObject] ++
        JsObject(Seq(
          "project" -> JsString(project),
          "task" -> JsString(task),
          "activity" -> JsString(activity),
          "activityLabel" -> JsString(activityLabel),
          "queueTime" -> queueTime.map(t => JsString(t.toString)).getOrElse(JsNull),
          "startTime" -> startTime.map(t => JsString(t.toString)).getOrElse(JsNull)
        ))
    }
  }

  implicit object ActivityExecutionMetaDataJsonFormat extends JsonFormat[ActivityExecutionMetaData] {

    private val STARTED_BY_USER = "startedByUser"
    private val QUEUED_AT = "queuedAt"
    private val STARTED_AT = "startedAt"
    private val FINISHED_AT = "finishedAt"
    private val CANCELLED_AT = "cancelledAt"
    private val CANCELLED_BY =  "cancelledBy"
    private val FINISH_STATUS = "finishStatus"

    override def read(value: JsValue)(implicit readContext: ReadContext): ActivityExecutionMetaData = {
      ActivityExecutionMetaData(
        startedByUser = stringValueOption(value, STARTED_BY_USER).map(SimpleUser),
        queuedAt = instantValueOption(value, QUEUED_AT),
        startedAt = instantValueOption(value, STARTED_AT),
        finishedAt = instantValueOption(value, FINISHED_AT),
        cancelledAt = instantValueOption(value, CANCELLED_AT),
        cancelledBy = stringValueOption(value, CANCELLED_BY).map(SimpleUser),
        finishStatus = optionalValue(value, FINISH_STATUS).map(StatusJsonFormat.read)
      )
    }

    override def write(value: ActivityExecutionMetaData)(implicit writeContext: WriteContext[JsValue]): JsValue = {
      Json.obj(
        STARTED_BY_USER -> value.startedByUser.map(_.uri),
        QUEUED_AT -> value.queuedAt,
        STARTED_AT -> value.startedAt,
        FINISHED_AT -> value.finishedAt,
        CANCELLED_AT -> value.cancelledAt,
        CANCELLED_BY -> value.cancelledBy.map(_.uri),
        FINISH_STATUS -> value.finishStatus.map(StatusJsonFormat.write)
      )
    }
  }

  class ActivityExecutionResultJsonFormat[T](implicit valueFormat: JsonFormat[T]) extends JsonFormat[ActivityExecutionResult[T]] {

    private val META_DATA = "metaData"
    private val VALUE = "value"

    override def read(value: JsValue)(implicit readContext: ReadContext): ActivityExecutionResult[T] = {
      ActivityExecutionResult(
        metaData = ActivityExecutionMetaDataJsonFormat.read(requiredValue(value, META_DATA)),
        resultValue = optionalValue(value, VALUE).map(valueFormat.read)
      )
    }

    override def write(value: ActivityExecutionResult[T])(implicit writeContext: WriteContext[JsValue]): JsValue = {
      Json.obj(
        META_DATA -> ActivityExecutionMetaDataJsonFormat.write(value.metaData),
        VALUE -> value.resultValue.map(valueFormat.write)
      )
    }
  }

}
